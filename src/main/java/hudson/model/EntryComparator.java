/*
 * Copyright (c) 1Spatial Group Ltd.
 */
/**
 * 
 */
package hudson.model;

import java.util.Comparator;

/**
 * Compares two {@link IViewEntry} by status and then name. 
 */
final class EntryComparator implements Comparator<IViewEntry>
{
    public int compare(IViewEntry o1, IViewEntry o2)
    {
        // first compare by status
        Result r1 = o1.getLastFinishedResult(); 
        Result r2 = o2.getLastFinishedResult();
        if (r1 != null && r2 != null)
        {
            if (r1.isBetterThan(r2))
            {
                return 1;
            }
            else if (r1.isWorseThan(r2))
            {
                return -1;
            }
        }

        // finally compare by name
        return o1.getName().compareTo(o2.getName());
    }
}