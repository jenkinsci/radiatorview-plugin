/*
 * Copyright (c) 1Spatial Group Ltd.
 */
package hudson.model;

import java.util.List;
import java.util.TreeSet;

public class RadiatorViewContents
{

    /** All jobs to be shown in the fail jobs area. */
    List<ViewEntry> failJobs;

    /** People who may have broken some of the builds. */
    List<String> allCulprits;

    /** Failures that have been claimed. */
    List<ViewEntry> claimedJobs;

    /** Jobs that are passing. */
    List<ViewEntry> passJobs;

    TreeSet<ViewEntry> passing = new TreeSet<ViewEntry>(new EntryComparator());
    TreeSet<ViewEntry> failing = new TreeSet<ViewEntry>(new EntryComparator());
    TreeSet<ViewEntry> claimed = new TreeSet<ViewEntry>(new EntryComparator());

    /** Some details about the build nodes */
    List<String> builders;

    public void addPassingBuild(ViewEntry build)
    {
        passing.add(build);
    }

    public void addFailingBuild(ViewEntry build)
    {
        failing.add(build);
    }

    public void addClaimedBuild(ViewEntry build)
    {
        claimed.add(build);
    }

    public TreeSet<ViewEntry> getClaimedBuilds()
    {
        return claimed;
    }

    public TreeSet<ViewEntry> getPassingJobs()
    {
        return passing;
    }

    public TreeSet<ViewEntry> getFailingJobs()
    {
        return failing;
    }

}
