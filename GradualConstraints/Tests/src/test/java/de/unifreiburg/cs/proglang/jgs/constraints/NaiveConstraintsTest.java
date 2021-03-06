package de.unifreiburg.cs.proglang.jgs.constraints;

import static de.unifreiburg.cs.proglang.jgs.util.Interop.asJavaOptional;
import static de.unifreiburg.cs.proglang.jgs.util.Interop.asJavaStream;
import static java.util.Arrays.equals;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.unifreiburg.cs.proglang.jgs.Code;
import de.unifreiburg.cs.proglang.jgs.constraints.TypeVars.TypeVar;
import de.unifreiburg.cs.proglang.jgs.typing.FlowConflict;
import de.unifreiburg.cs.proglang.jgs.typing.TagMap;
import de.unifreiburg.cs.proglang.jgs.util.Interop;
import org.junit.Before;
import org.junit.Test;

import de.unifreiburg.cs.proglang.jgs.constraints.secdomains.LowHigh.Level;
import scala.Option;
import soot.IntType;
import soot.SootField;

import static de.unifreiburg.cs.proglang.jgs.TestDomain.*;
import static de.unifreiburg.cs.proglang.jgs.constraints.CTypes.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static java.util.Collections.*;

public class NaiveConstraintsTest {

    private TypeVars tvars;
    private SomeConstraintSets cs;
    private Code code;

    @Before
    public void setUp() {

        tvars = new TypeVars();
        cs = new SomeConstraintSets(tvars);
        code =  new Code(tvars);
    }

    @Test
    public void testSatisfiability() {
        assertThat(makeNaive(Collections.emptyList()), is(sat()));
        assertThat(cs.x0_le_x1_le_x2_le_x3_le_x1, is(sat()));
        assertThat(cs.x1_le_H_le_x2__x1_le_x2, is(sat()));
        assertThat(cs.pub_le_x_le_dyn, is(sat()));
        assertThat("SAT(x < HIGH, x < ?)",
                   makeNaive(Arrays.asList(leC(cs.x1, literal(DYN)),
                                           leC(cs.x1,
                                               literal(THIGH)))), is(sat()));

        assertThat("~SAT(HIGH < LOW)",
                   makeNaive(Collections.singletonList(leC(literal(THIGH),
                                                           literal(TLOW)))), not(is(sat())));
        assertThat("~SAT(HIGH < x, x < y , y < LOW)",
                   makeNaive(asList(leC(literal(THIGH), cs.x1),
                                    leC(cs.x1, cs.x2),
                                    leC(cs.x2, literal(TLOW)))), not(is(sat())));
        assertThat("SAT(LOW < x, x < ?)",
                   makeNaive(asList(leC(cs.x1, literal(DYN)),
                                    leC(literal(TLOW), cs.x1))), not(is(sat())));
    }

    @Test
    public void testSatAssignments() {
        Optional<Assignment<Level>> result, expected;
        result =
                asJavaOptional(cstrs.satisfyingAssignment(makeNaive(Arrays.asList(leC(cs.x1, literal(THIGH)),
                                                                   leC(literal(THIGH), cs.x2),
                                                                   leC(cs.x2,
                                                                       cs.x1))), Collections.emptySet()));
        expected = Optional.of(Assignments.builder(cs.v1, THIGH)
                                          .add(cs.v2, THIGH)
                                          .build());
        assertEquals("x1 = x2 = HIGH", expected, result);

        result =
                asJavaOptional(cstrs.satisfyingAssignment(makeNaive(Arrays.asList(leC(cs.x1, literal(DYN)),
                                                                   leC(cs.x1,
                                                                       literal(THIGH)))), Collections.emptySet()));
        expected = Optional.of(Assignments.builder(cs.v1, PUB).build());
        assertEquals("x = pub", expected, result);
    }

    @Test
    public void testImplications() {
        /*
         * - more constraints imply less constraints 
         */
        ConstraintSet<Level> more = makeNaive(asList(leC(cs.x1, literal(THIGH)),
                                                     leC(literal(THIGH), cs.x2),
                                                     leC(cs.x2, cs.x1)));
        ConstraintSet<Level> less = makeNaive(asList(leC(cs.x1, literal(THIGH)),
                                                     leC(literal(THIGH),
                                                         cs.x2)));
        assertThat("more => less", more, implies(less));
        assertThat("less /=> (significant) more", less, not(implies(more)));

        assertThat("more => more+trivial",
                   more, implies(more.add(leC(cs.x1, cs.x1))
                                     .add(leC(literal(PUB), cs.x2))));

        /*
         * - < LOW constraints imply < High constraints
         */
        ConstraintSet<Level> lowerLess =
                makeNaive(asList(leC(cs.x1, literal(TLOW)),
                                 leC(literal(THIGH), cs.x2),
                                 leC(cs.x1, cs.x2)));
        assertThat("lowLess => less", lowerLess, implies(less));
        assertThat("loweLess /=> less", less, not(implies(lowerLess)));

        /*
         * - unsat constraints imply arbitrary constraints 
         */
        ConstraintSet<Level> unsat =
                makeNaive(Arrays.asList(leC(literal(THIGH), cs.x1),
                                        leC(cs.x1, cs.x2),
                                        leC(cs.x2, literal(TLOW))));
        assertThat("unsat => more", unsat, implies(more));
        assertThat("unsat => less", unsat, implies(less));
        assertThat("unsat => lowerLess", unsat, implies(lowerLess));
        assertThat("unsat => unsat", unsat, implies(unsat));
    }

    @Test(timeout = 1000)
    public void testLeClosure() {
        Set<Constraint<Level>> tmp =
                asJavaStream(makeNaive(asList(leC(cs.x1, cs.x2), leC(cs.x2, cs.x3))).stream()).collect(toSet());
        ConstraintSet<Level> closed = makeNaive(NaiveConstraints$.MODULE$.close(tmp));
        ConstraintSet<Level> expected =
                makeNaive(asList(leC(cs.x1, cs.x2), leC(cs.x2, cs.x3), leC(cs.x1, cs.x3)));
        assertThat(closed, is(equivalent(expected)));
    }

    public void testLeClosureWithConcreteTypes() {
        Set<Constraint<Level>> tmp =
                Stream.of(leC(cs.x1, cs.x2), leC(cs.x2, cs.x3), leC(literal(THIGH), literal(TLOW))).collect(toSet());
        ConstraintSet<Level> closed = makeNaive(NaiveConstraints$.MODULE$.close(tmp));
        ConstraintSet<Level> expected =
                makeNaive(asList(leC(cs.x1, cs.x2), leC(cs.x2, cs.x3), leC(cs.x1, cs.x3)));
    }


    @Test
    public void testCompClosure() {
        Set<Constraint<Level>> tmp =
                asJavaStream(makeNaive(asList(compC(cs.x1, cs.x2), compC(cs.x2, cs.x3))).stream()).collect(toSet());
        ConstraintSet<Level> closed = makeNaive(NaiveConstraints$.MODULE$.close(tmp));
        // closing compatibility constraints does not have an effect
        ConstraintSet<Level> expected =
                makeNaive(asList(compC(cs.x1, cs.x2), compC(cs.x2, cs.x3)));

        assertThat(closed, is(equivalent(expected)));

        // closing compatibility constraints does not have an effect. This is the same test as above
        tmp =
                asJavaStream(makeNaive(asList(compC(cs.x2, cs.x1), compC(cs.x2, cs.x3))).stream()).collect(toSet());
        closed = makeNaive(NaiveConstraints$.MODULE$.close(tmp));
        expected = makeNaive(asList(compC(cs.x1, cs.x2), compC(cs.x2, cs.x3)));

        assertThat(closed, is(equivalent(expected)));

        tmp =
                asJavaStream(makeNaive(asList(leC(cs.x1, cs.x2), leC(cs.x3, cs.x2))).stream()).collect(toSet());
        closed = makeNaive(NaiveConstraints$.MODULE$.close(tmp));
        expected =
                makeNaive(asList(leC(cs.x1, cs.x2), leC(cs.x3, cs.x2), compC(cs.x1, cs.x3)));

        assertThat("CompClosure failed", closed, is(equivalent(expected)));
        Set<TypeVar> projSet = Stream.of(cs.v1, cs.v3).collect(toSet());
        assertThat("Projection failed", makeNaive(tmp).projectTo(projSet), is(equivalent(closed.projectTo(projSet))));

        /* sig = { x1 <= x0, x1 ~ x3 }
           cset = { x1 <= x2, x3 <= x2, x1 <= x0 }
        */
        tmp = asJavaStream(makeNaive(asList(leC(cs.x1, cs.x2), leC(cs.x3, cs.x2), leC(cs.x1, cs.x0))).stream()).collect(toSet());
        projSet = Stream.of(cs.v1, cs.v0, cs.v3).collect(Collectors.toSet());
        assertThat("CompClosure1 failed", NaiveConstraints.minimize(makeNaive(tmp).projectTo(projSet)), is(equivalent(makeNaive(Stream.of(leC(cs.x1, cs.x0), compC(cs.x1, cs.x3)).collect(Collectors.toSet())))));
    }

    void assertProjection(Collection<Constraint<Level>> cs, Collection<TypeVar> vars, Collection<Constraint<Level>> expectedSet) {
        ConstraintSet<Level> projected =
                makeNaive(cs).projectTo(new HashSet<>(vars));
        ConstraintSet<Level> expected = makeNaive(expectedSet);
        assertThat(String.format("%s projected to %s", cs, vars), projected
                , is(equivalent(expected)));
    }

    @Test
    public void testProjection1() {
        Set<Constraint<Level>> tmp =
                Stream.of(leC(cs.x1, cs.x2), leC(cs.x2, cs.x3)).collect(toSet());
        assertProjection(tmp, asList(cs.v1, cs.v3), asList(leC(cs.x1, cs.x3)));

        tmp = Stream.of(leC(cs.x1, cs.x2), leC(cs.x2, cs.x3)).collect(toSet());
        assertProjection(tmp, asList(cs.v1), Collections.<Constraint<Level>>emptySet());

        tmp = Stream.of(leC(cs.x1, cs.x2), leC(cs.x2, cs.x3)).collect(toSet());
        assertProjection(tmp, asList(cs.v1, cs.v0), Collections.<Constraint<Level>>emptySet());

        tmp =
                Stream.of(leC(cs.x1, cs.x2), leC(cs.x0, cs.x3), leC(cs.x1, cs.x3)).collect(toSet());
        assertProjection(tmp, asList(cs.v1, cs.v0), Stream.of(compC(cs.x0, cs.x1)).collect(toSet()));

        assertProjection(tmp, asList(cs.v1, cs.v0), Stream.of(compC(cs.x1, cs.x0)).collect(toSet()));

        assertProjection(Stream.of(leC(literal(TLOW), variable(tvars.ret()))).collect(toList()),
                         Stream.of(tvars.ret()).collect(toList()),
                         Stream.of(leC(literal(TLOW), variable(tvars.ret()))).collect(toList()));
    }

    // TODO: need a property test that "forall s cs1 cs2. equivalent(cs1, cs2) => equivalent(cs1.projectTo(s), cs2.projectTo(s))"
    // TODO: need a property test that "forall s cs. equivalent(minimize(cs), cs)"

    @Test
    public void testMinimize() {
        Set<Constraint<Level>> tmp =
                Stream.of(leC(cs.x1, cs.x2), leC(cs.x2, cs.x3), compC(cs.x3, cs.x2), dimplC(cs.x1, cs.x2)).collect(toSet());
        Set<Constraint<Level>> expected =
                Stream.of(leC(cs.x1, cs.x2), leC(cs.x2, cs.x3)).collect(toSet());
        assertThat(makeNaive(NaiveConstraints$.MODULE$.minimize(tmp)), is(equivalent(makeNaive(expected))));

        assertThat("minimize comp", (NaiveConstraints.minimize(makeNaive(singletonList(compC(cs.x2, cs.x3))))), is(equivalent(makeNaive(singletonList(compC(cs.x2, cs.x3))))));

        tmp = asJavaStream(makeNaive(asList(leC(cs.x1, cs.x2), leC(cs.x3, cs.x2), leC(cs.x1, cs.x0))).stream()).collect(toSet());
        Set<TypeVar> projSet = Stream.of(cs.v1, cs.v0, cs.v3).collect(Collectors.toSet());
        assertThat("just minimize", NaiveConstraints.minimize(makeNaive(tmp)), is(equivalent(makeNaive(tmp))));
        assertThat("minimize and project", NaiveConstraints.minimize(makeNaive(tmp).projectTo(projSet)), is(equivalent(makeNaive(tmp).projectTo(projSet))));
    }

    @Test
    public void testEquivalences() {
        ConstraintSet<Level> cs11 =
                makeNaive(asList(leC(cs.x0, cs.x1), leC(cs.x2, cs.x1), compC(cs.x2, cs.x0)));
        ConstraintSet<Level> cs22 =
                makeNaive(asList(leC(cs.x0, cs.x1), leC(cs.x2, cs.x1)));

        assertThat(cs11, is(equivalent(cs22)));

        ConstraintSet<Level> cs1 =
                makeNaive(asList(leC(cs.x0, cs.x1), compC(cs.x0, cs.x2)));
        ConstraintSet<Level> cs2 =
                makeNaive(asList(leC(cs.x0, cs.x1), compC(cs.x1, cs.x2)));

        assertThat(cs1, is(not(equivalent(cs2))));
    }

    @Test
    public void testSubsumption() {
        assertThat(makeNaive(singletonList(leC(CHIGH, CTypes.variable(tvars.ret())))), subsumes(makeNaive(singletonList(leC(CLOW, CTypes.variable(tvars.ret()))))));
        assertThat(makeNaive(asList(leC(CLOW, cs.x1), leC(cs.x1, tvars.ret()), leC(CHIGH, cs.x2))), refines(tvars, makeNaive(singletonList(leC(CLOW, CTypes.variable(tvars.ret()))))));
        assertThat(makeNaive(singletonList(leC(CLOW, CTypes.variable(tvars.ret())))), minimallySubsumes(makeNaive(singletonList(leC(CLOW, CTypes.variable(tvars.ret()))))));
        assertThat(makeNaive(singletonList(leC(CLOW, CTypes.variable(tvars.ret())))), minimallySubsumes(makeNaive(asList(leC(CLOW, cs.x1), leC(cs.x1, tvars.ret()), leC(CHIGH, cs.x2)))));
        assertThat(makeNaive(singletonList(leC(CHIGH, CTypes.variable(tvars.ret())))), not(minimallySubsumes(makeNaive(singletonList(leC(CLOW, CTypes.variable(tvars.ret())))))));
    }

    @Test
    public void testEmptyRefinement() {
        assertThat("with trivial constraints", makeNaive(singletonList(leC(CLOW, variable(tvars.ret())))),
                   notRefines(tvars, makeNaive(singletonList(leC(variable(tvars.ret()), variable(tvars.ret()))))));
    }

    @Test
    public void testCausFinding_HField_LField_1() {
        SootField lowField = new SootField("lowField", IntType.v());
        SootField highField = new SootField("highField", IntType.v());
        code.testClass.addField(lowField);
        code.testClass.addField(highField);
        Constraint<Level> c1 = leC(literal(THIGH), variable(cs.v0));
        Constraint<Level> c2 = leC(variable(cs.v0), literal(TLOW));
        TagMap<Level> tags = TagMap.of(c1, new TypeVarTags.Field(highField))
                .add(c2,  new TypeVarTags.Field(lowField));
        ConstraintSet<Level> cs = makeNaive(asList(c1, c2));
        assertThat("Is satisfiable!", cs, is(not(sat())));
        assertThat(cs.findConflictCause(tags), is(asList(new FlowConflict<>(THIGH, new TypeVarTags.Field(highField),
                                                  TLOW, new TypeVarTags.Field(lowField)))));
    }

    @Test
    public void testLowerBounds() {
        ConstraintSet<Level> cset = makeNaive(asList(leC(literal(THIGH), variable(cs.v0)),
                                                   leC(variable(cs.v1), variable(cs.v0)),
                                                   leC(literal(TLOW), variable(cs.v1)),
                                                     leC(variable(cs.v1), variable(cs.v2))));
        Set<CTypeViews.CTypeView<Level>> res = asJavaStream(cset.lowerBounds(cs.v0).iterator()).collect(toSet());
        List<CTypeViews.CTypeView<Level>> expected = asList(
                CTypes.<Level>literal(THIGH).inspect(),
                CTypes.<Level>variable(cs.v1).inspect(),
                CTypes.<Level>literal(TLOW).inspect());
        assertThat(res, is(expected.stream().collect(toSet())));
    }

    @Test
    public void testGreatestLowerBounds() {
        ConstraintSet<Level> cset;
        Option<TypeViews.TypeView<Level>> res;
        Option<TypeViews.TypeView<Level>> expected;

        cset = makeNaive(asList(leC(literal(THIGH), variable(cs.v0)),
                                                     leC(variable(cs.v1), variable(cs.v0)),
                                                     leC(literal(TLOW), variable(cs.v1)),
                                                     leC(variable(cs.v1), variable(cs.v2))));
        res = cset.greatestLowerBound(cs.v0);
        expected = Option.apply(THIGH);

        assertThat(res, is(expected));

        cset = makeNaive(asList(leC(literal(TLOW), variable(cs.v0)),
                                leC(variable(cs.v1), variable(cs.v0)),
                                leC(literal(THIGH), variable(cs.v1)),
                                leC(variable(cs.v1), variable(cs.v2))));
        res = cset.greatestLowerBound(cs.v0);
        expected = Option.apply(THIGH);

        assertThat(res, is(expected));

        cset = makeNaive(asList(leC(literal(DYN), variable(cs.v0)),
                                leC(variable(cs.v1), variable(cs.v0))  ,
                                leC(literal(TLOW), variable(cs.v1)),
                                leC(variable(cs.v1), variable(cs.v2))));
        res = cset.greatestLowerBound(cs.v0);
        expected = Option.empty();

        assertThat(res, is(expected));

        cset = makeNaive(asList(leC(variable(cs.v1), variable(cs.v0)),
                                leC(variable(cs.v1), variable(cs.v0))  ,
                                leC(variable(cs.v2), variable(cs.v1)),
                                leC(variable(cs.v1), variable(cs.v2))));
        res = cset.greatestLowerBound(cs.v0);
        expected = Option.empty();

        assertThat(res, is(expected));
    }
}
