/*
 * Copyright (c) 1Spatial Group Ltd.
 */
/**
 * 
 */
package hudson.model;

import java.util.Comparator;

final class EntryComparator implements Comparator<ViewEntry>
{
    public int compare(ViewEntry o1, ViewEntry o2)
    {
        // first compare by status
        Result r1 = RadiatorView.getResult(o1.getJob());
        Result r2 = RadiatorView.getResult(o2.getJob());
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