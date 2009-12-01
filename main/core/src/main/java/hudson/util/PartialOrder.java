package hudson.util;

/**
 * Partial order relationship, which allows two elements to be incomparable
 * (in addition to normal full order relationship.)
 *
 * <p>
 * Nodes in a DAG has a partial order, for example. 
 *
 * @author Kohsuke Kawaguchi
 */
public enum PartialOrder {
    LESS_THAN,
    GREATER_THAN,
    EQUAL,
    INCOMPARABLE;

    /**
     * Compares two ints and determines the order.
     */
    public static PartialOrder from(int i, int j) {
        if(i<j) return LESS_THAN;
        if(i>j) return GREATER_THAN;
        return EQUAL;
    }

    /**
     * Obtains a partial order from two set relationship.
     *
     * @param lt
     *      If 'that' contains something that's not in 'this'
     *      (intuitively, this makes 'this' smaller than 'that')
     * @param gt
     *      If 'this' contains something that's not in 'that'.
     *      (intuitively, this makes 'this' bigger than 'that'.)
     */
    public static PartialOrder from(boolean lt, boolean gt) {
        if(lt&&gt)  return INCOMPARABLE;
        if(lt)      return LESS_THAN;
        if(gt)      return GREATER_THAN;
        return EQUAL;
    }
}
