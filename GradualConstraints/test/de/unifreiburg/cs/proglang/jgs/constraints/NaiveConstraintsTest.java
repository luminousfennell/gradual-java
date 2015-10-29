package de.unifreiburg.cs.proglang.jgs.constraints;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import de.unifreiburg.cs.proglang.jgs.constraints.secdomains.LowHigh.Level;

import static de.unifreiburg.cs.proglang.jgs.constraints.TestDomain.*;
import static de.unifreiburg.cs.proglang.jgs.constraints.CTypes.*;
import static java.util.Arrays.asList;

public class NaiveConstraintsTest {

    private TypeVars tvars;
    private SomeConstraintSets cs;

    @Before public void setUp() {

        tvars = new TypeVars("x");
        cs = new SomeConstraintSets(tvars);
    }

    @Test public void testSatisfiability() {
        assertThat(makeNaive(Collections.emptyList()), is(sat()));
        assertThat(cs.x0_le_x1_le_x2_le_x3_le_x1, is(sat()));
        assertThat(cs.x1_le_H_le_x2__x1_le_x2, is(sat()));
        assertThat(cs.pub_le_x_le_dyn, is(sat()));
        assertTrue("SAT(x < HIGH, x < ?)",
                   makeNaive(Arrays.asList(leC(cs.x1, literal(DYN)),
                                           leC(cs.x1,
                                               literal(THIGH)))).isSat());

        assertFalse("~SAT(HIGH < LOW)",
                    makeNaive(Collections.singletonList(leC(literal(THIGH),
                                                            literal(TLOW)))).isSat());
        assertFalse("~SAT(HIGH < x, x < y , y < LOW)",
                    makeNaive(asList(leC(literal(THIGH), cs.x1),
                                     leC(cs.x1, cs.x2),
                                     leC(cs.x2, literal(TLOW)))).isSat());
        assertFalse("SAT(LOW < x, x < ?)",
                    makeNaive(asList(leC(cs.x1, literal(DYN)),
                                     leC(literal(TLOW), cs.x1))).isSat());
    }

    @Test public void testSatAssignments() {
        Optional<Assignment<Level>> result, expected;
        result = makeNaive(Arrays.asList(leC(cs.x1, literal(THIGH)),
                                         leC(literal(THIGH), cs.x2),
                                         leC(cs.x2,
                                             cs.x1))).satisfyingAssignment(
                Collections.emptySet());
        expected = Optional.of(Assignments.builder(cs.v1, THIGH)
                                          .add(cs.v2, THIGH)
                                          .build());
        assertEquals("x1 = x2 = HIGH", expected, result);

        result = makeNaive(Arrays.asList(leC(cs.x1, literal(DYN)),
                                         leC(cs.x1,
                                             literal(THIGH)))).satisfyingAssignment(
                Collections.emptySet());
        expected = Optional.of(Assignments.builder(cs.v1, PUB).build());
        assertEquals("x = pub", expected, result);
    }

    @Test public void testImplications() {
        /*
         * - more constraints imply less constraints 
         */
        ConstraintSet<Level> more = makeNaive(asList(leC(cs.x1, literal(THIGH)),
                                                     leC(literal(THIGH), cs.x2),
                                                     leC(cs.x2, cs.x1)));
        ConstraintSet<Level> less = makeNaive(asList(leC(cs.x1, literal(THIGH)),
                                                     leC(literal(THIGH),
                                                         cs.x2)));
        assertTrue("more => less", more.implies(less));
        assertFalse("less /=> (significant) more", less.implies(more));

        assertTrue("more => more+trivial",
                   more.implies(more.add(leC(cs.x1, cs.x1))
                                    .add(leC(literal(PUB), cs.x2))));

        /*
         * - < LOW constraints imply < High constraints
         */
        ConstraintSet<Level> lowerLess =
                makeNaive(asList(leC(cs.x1, literal(TLOW)),
                                 leC(literal(THIGH), cs.x2),
                                 leC(cs.x1, cs.x2)));
        assertTrue("lowLess => less", lowerLess.implies(less));
        assertFalse("loweLess => less", less.implies(lowerLess));

        /*
         * - unsat constraints imply arbitrary constraints 
         */
        ConstraintSet<Level> unsat =
                makeNaive(Arrays.asList(leC(literal(THIGH), cs.x1),
                                        leC(cs.x1, cs.x2),
                                        leC(cs.x2, literal(TLOW))));
        assertTrue("unsat => more", unsat.implies(more));
        assertTrue("unsat => less", unsat.implies(less));
        assertTrue("unsat => lowerLess", unsat.implies(lowerLess));
        assertTrue("unsat => unsat", unsat.implies(unsat));
    }

}