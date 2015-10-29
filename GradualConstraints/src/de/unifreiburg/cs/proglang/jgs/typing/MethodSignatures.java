package de.unifreiburg.cs.proglang.jgs.typing;

import de.unifreiburg.cs.proglang.jgs.constraints.*;
import soot.jimple.ParameterRef;

import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;

import static de.unifreiburg.cs.proglang.jgs.constraints.TypeDomain.*;
import static java.util.Arrays.asList;
import static de.unifreiburg.cs.proglang.jgs.constraints.CTypes.*;
import static de.unifreiburg.cs.proglang.jgs.constraints.TypeVars.*;


/**
 * Method signtures. Internal representation of method signatures of the form
 * <p>
 * M where <signature-constraints> and <effect>
 * <p>
 * Signature constraints are similar to regular constraints but instead of
 * relating type variables, they relate special symbols, which are:
 * <p>
 * - Parameter names - "@return" - Security Levels
 * <p>
 * Effects are just sets of security Levels
 *
 * @author fennell
 */
public class MethodSignatures<Level> {

    private final Constraints<Level> cstrs;

    public MethodSignatures(Constraints<Level> cstrs) {
        this.cstrs = cstrs;
    }
    
    /* Effects */

    public static <Level> Effects<Level> emptyEffect() {
        return new Effects<>(new HashSet<>());
    }

    @SafeVarargs
    public static <Level> Effects<Level> union(Effects<Level>... effectSets) {
        HashSet<Type<Level>> result = new HashSet<>();
        for (Effects<Level> es : effectSets) {
            result.addAll(es.effectSet);
        }
        return new Effects<>(result);
    }

    public final static class Effects<Level> {
        private final HashSet<Type<Level>> effectSet;

        private Effects(HashSet<Type<Level>> effects) {
            this.effectSet = effects;
        }

        @SafeVarargs
        public final Effects<Level> add(Type<Level>... types) {
            HashSet<Type<Level>> result = new HashSet<>(this.effectSet);
            result.addAll(asList(types));
            return new Effects<>(result);
        }
    }

    /* Signatures */

    public SigConstraint<Level> le(Symbol<Level> lhs, Symbol<Level> rhs) {
        return new SigConstraint<>(lhs, rhs, (ct1, ct2) -> cstrs.le(ct1, ct2));
    }

    public SigConstraint<Level> comp(Symbol<Level> lhs, Symbol<Level> rhs) {
        return new SigConstraint<>(lhs, rhs, (ct1, ct2) -> cstrs.le(ct1, ct2));
    }

    public SigConstraint<Level> dimpl(Symbol<Level> lhs, Symbol<Level> rhs) {
        return new SigConstraint<>(lhs, rhs, (ct1, ct2) -> cstrs.le(ct1, ct2));
    }

    public static <Level> Symbol<Level> param(ParameterRef me) {
        return new Param<>(me);
    }

    // TODO: make a singleton out of this
    public static <Level> Symbol<Level> ret() {
        return new Return<>();
    }

    public static <Level> Symbol<Level> literal(Type<Level> t) {
        return new Literal<>(t);
    }

    /* Signature constraints */
    public static class SigConstraint<Level> {

        private final Symbol<Level> lhs;
        private final Symbol<Level> rhs;
        private final BiFunction<CType<Level>, CType<Level>, Constraint<Level>> toConstraint;

        private SigConstraint(Symbol<Level> lhs,
                              Symbol<Level> rhs,
                              BiFunction<CType<Level>, CType<Level>, Constraint<Level>> toConstraint) {
            super();
            this.lhs = lhs;
            this.rhs = rhs;
            this.toConstraint = toConstraint;
        }

        public Constraint<Level> toTypingConstraints(Map<Symbol<Level>, TypeVar> tvarMapping) {
            return toConstraint.apply(lhs.toCType(tvarMapping),
                    rhs.toCType(tvarMapping));
        }
    }

        /* Symbols: */

    public static abstract class Symbol<Level> {
        @Override
        public abstract String toString();

        public abstract CType<Level> toCType(Map<Symbol<Level>, TypeVar> tvarMapping);
    }

    public static class Param<Level> extends Symbol<Level> {
        private final ParameterRef me;

        private Param(ParameterRef me) {
            super();
            this.me = me;
        }


        @Override
        public String toString() {
            return String.format("@%s", this.me.toString());
        }

        @Override
        public CType<Level> toCType(Map<Symbol<Level>, TypeVar> tvarMapping) {
            return Optional.ofNullable(tvarMapping.get(this))
                    .map(CTypes::<Level>variable)
                    .orElseThrow(() -> new NoSuchElementException(String.format("No mapping for %s: %s",
                            this.me.toString(),
                            tvarMapping.toString())));
        }
    }


    public static class Return<Level> extends Symbol<Level> {

        private Return() {
        }

        @Override
        public String toString() {
            return "@return";
        }

        @Override
        public CType<Level> toCType(Map<Symbol<Level>, TypeVar> tvarMapping) {
            return Optional.ofNullable(tvarMapping.get(this))
                    .map(CTypes::<Level>variable)
                    .orElseThrow(() -> new NoSuchElementException(String.format("No mapping for %s: %s",
                            this.toString(),
                            tvarMapping.toString())));
        }

        @Override
        // TODO: should be a singleton
        public boolean equals(Object other) {
            return this.getClass().equals(other.getClass());
        }

        @Override
        public int hashCode() {
            return this.getClass().hashCode();
        }

    }

    public static class Literal<Level> extends Symbol<Level> {
        final Type<Level> me;

        private Literal(Type<Level> me) {
            super();
            this.me = me;
        }

        @Override
        public String toString() {
            return me.toString();
        }

        @Override
        public CType<Level> toCType(Map<Symbol<Level>, TypeVar> tvarMapping) {
            return CTypes.literal(this.me);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((me == null) ? 0 : me.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Literal other = (Literal) obj;
            if (me == null) {
                if (other.me != null)
                    return false;
            } else if (!me.equals(other.me))
                return false;
            return true;
        }
    }
}