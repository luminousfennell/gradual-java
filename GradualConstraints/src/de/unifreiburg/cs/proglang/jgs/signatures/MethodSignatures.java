package de.unifreiburg.cs.proglang.jgs.signatures;

import de.unifreiburg.cs.proglang.jgs.constraints.*;
import de.unifreiburg.cs.proglang.jgs.signatures.Symbol;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unifreiburg.cs.proglang.jgs.constraints.TypeDomain.*;
import static java.util.Arrays.asList;
import static de.unifreiburg.cs.proglang.jgs.constraints.CTypes.*;
import static de.unifreiburg.cs.proglang.jgs.constraints.TypeVars.*;

/**
 * Method signtures. Internal representation of method signatures of the form
 * <p>
 * M where <signature-constraints> and <effect>
 * <p>
 * Signature constraints are similar to regular constraints but instead of relating type variables, they relate special
 * symbols, which are:
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

    /**
     * Signatures: constraints + effects
     */
    public static <Level> Signature<Level> makeSignature(SigConstraintSet<Level> constraints, Effects<Level> effects) {
        return new Signature<>(constraints, effects);
    }

    public static class Signature<Level> {
        public final SigConstraintSet<Level> constraints;
        public final Effects<Level> effects;

        private Signature(SigConstraintSet<Level> constraints, Effects<Level> effects) {
            this.constraints = constraints;
            this.effects = effects;
        }
    }

    /* Effects */
    public static <Level> Effects<Level> emptyEffect() {
        return new Effects<>(new HashSet<>());
    }

    public static <Level> Effects<Level> effects(Type<Level> type, Type<Level>... types) {
        return MethodSignatures.<Level>emptyEffect().add(type, types);
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

        public final Effects<Level> add(Type<Level> type, Type<Level>... types) {
            HashSet<Type<Level>> result = new HashSet<>(this.effectSet);
            result.add(type); result.addAll(asList(types));
            return new Effects<>(result);
        }
    }

    /* Signatures */
    public static <Level> SigConstraintSet<Level> signatureConstraints(Collection<SigConstraint<Level>> sigSet) {
        return new SigConstraintSet<>(sigSet);
    }

    public SigConstraintSet<Level> toSignatureConstraintSet(ConstraintSet<Level> constraints, Map<TypeVar, Symbol.Param> params, TypeVar retVar) {
        Set<TypeVar> relevantVars = Stream.concat(Collections.singleton(retVar).stream(), params.keySet().stream()).collect(Collectors.toSet());

        CTypeSwitch<Level, Symbol<Level>> toSymbol = new CTypeSwitch<Level, Symbol<Level>>() {
            @Override
            public Symbol<Level> caseLiteral(Type<Level> t) {
                return Symbol.literal(t);
            }

            @Override
            public Symbol<Level> caseVariable(TypeVar v) {
                Symbol.Param p = Optional.ofNullable(params.get(v)).orElseThrow(() -> new NoSuchElementException(String.format("Type variable %s not found in parameter map: %s", v, params)));
                return p;
            }
        };

        return signatureConstraints(constraints.projectTo(params.keySet()).stream().map(c -> {
            Symbol<Level> lhs = c.getLhs().accept(toSymbol);
            Symbol<Level> rhs = c.getRhs().accept(toSymbol);
            switch (c.getConstraintKind()) {
                case LE:
                    return le(lhs, rhs);
                case COMP:
                    return comp(lhs, rhs);
                case DIMPL:
                    return dimpl(lhs, rhs);
                default:
                    throw new IllegalArgumentException(String.format("Unexpected case for ConstraintKind: %s", c));
            }
        }).collect(Collectors.toSet()));
    }


    public SigConstraint<Level> le(Symbol<Level> lhs, Symbol<Level> rhs) {
        return new SigConstraint<>(lhs, rhs, cstrs::le);
    }

    public SigConstraint<Level> comp(Symbol<Level> lhs, Symbol<Level> rhs) {
        return new SigConstraint<>(lhs, rhs, cstrs::le);
    }

    public SigConstraint<Level> dimpl(Symbol<Level> lhs, Symbol<Level> rhs) {
        return new SigConstraint<>(lhs, rhs, cstrs::le);
    }

    /* Signature constraints */
    public static class SigConstraintSet<Level> {
        private final Set<SigConstraint<Level>> sigSet;

        private SigConstraintSet(Collection<SigConstraint<Level>> sigSet) {
            this.sigSet = new HashSet<>(sigSet);
        }

        public Stream<Constraint<Level>> toTypingConstraints(Map<Symbol<Level>, TypeVar> mapping) {
            return this.sigSet.stream()
                    .map(c -> c.toTypingConstraint(mapping));
        }
    }

    public static class SigConstraint<Level> {

        private final Symbol<Level> lhs;
        private final Symbol<Level> rhs;
        private final BiFunction<CType<Level>, CType<Level>, Constraint<Level>>
                toConstraint;

        private SigConstraint(Symbol<Level> lhs,
                              Symbol<Level> rhs,
                              BiFunction<CType<Level>, CType<Level>, Constraint<Level>> toConstraint) {
            super();
            this.lhs = lhs;
            this.rhs = rhs;
            this.toConstraint = toConstraint;
        }

        public Constraint<Level> toTypingConstraint(Map<Symbol<Level>, TypeVar> tvarMapping) {
            return toConstraint.apply(lhs.toCType(tvarMapping),
                    rhs.toCType(tvarMapping));
        }
    }

        /* Symbols: */

}