package de.unifreiburg.cs.proglang.jgs.instrumentation;


import soot.Local;
import soot.jimple.ThisRef;

/**
 * JGS' variables are what Jimple calls "Local" "ThisRef" and "ParameterRef".
 * This class wraps the three Jimple classes into one type, namely Var<?>.
 * 
 * @author fennell
 *
 * @param <T>
 */
public class Var<T> {
    public final T repr;


    // factory methods
    public static Var<Local> fromLocal(Local l) {
      return new Var<>(l);
    }
    public static Var<ThisRef> fromThis(ThisRef tr) {
        return new Var<>(tr);
    }

    public static Var<Integer> fromParam(Integer i) {
        return new Var<>(i);
    }

    public Var(T me) {
        super();
        this.repr = me;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((repr == null) ? 0 : repr.hashCode());
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
        Var other = (Var) obj;
        if (repr == null) {
            if (other.repr != null)
                return false;
        } else if (!repr.equals(other.repr))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return  repr.toString();
    }

}
