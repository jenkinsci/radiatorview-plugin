package hudson.model;

import hudson.Functions;
import hudson.tasks.test.AbstractTestResultAction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a job to be shown in a view. Based heavily on the XFPanelEntry in
 * XFPanel plugin.
 * 
 * @author jrenaut
 */
public final class ViewEntry
{

    /**
     * 
     */
    private final RadiatorView radiatorView;

    private Job<?, ?> job;

    private String backgroundColor;

    private String color;

    private Boolean broken;

    private Boolean building = false;

    private Boolean queued = false;

    private Integer queueNumber;

    /**
     * C'tor
     * 
     * @param job
     *            the job to be represented
     * @param radiatorView
     *            TODO
     */
    public ViewEntry(RadiatorView radiatorView, Job<?, ?> job)
    {
        this.radiatorView = radiatorView;
        this.job = job;
        this.findStatus();
    }

    /**
     * @return the job
     */
    public Job<?, ?> getJob()
    {
        return this.job;
    }

    /**
     * @return the job's name
     */
    public String getName()
    {
        return job.getName();
    }

    /**
     * @return if this job is queued for build
     */
    public Boolean getQueued()
    {
        return this.job.isInQueue();
    }

    /**
     * @return the job's queue number, if any
     */
    public Integer getQueueNumber()
    {
        return this.radiatorView.placeInQueue.get(this.job.getQueueItem());
    }

    /**
     * @return background color for this job
     */
    public String getBackgroundColor()
    {
        return this.backgroundColor;
    }

    /**
     * @return foreground color for this job
     */
    public String getColor()
    {
        return this.color;
    }

    /**
     * @return true se o último build está quebrado
     */
    public Boolean getBroken()
    {
        return this.broken;
    }

    /**
     * @return true if this job is currently being built
     */
    public Boolean getBuilding()
    {
        return this.building;
    }

    /**
     * @return the URL for the last build
     */
    public String getUrl()
    {
        return this.job.getUrl() + "lastBuild";
    }

    /**
     * @return a list will all the currently building runs for this job.
     */
    public List<Run<?, ?>> getBuildsInProgress()
    {
        List<Run<?, ?>> runs = new ArrayList<Run<?, ?>>();

        Run<?, ?> run = this.job.getLastBuild();
        if (run.isBuilding())
        {
            runs.add(run);
        }

        Run<?, ?> prev = run.getPreviousBuildInProgress();
        while (prev != null)
        {
            runs.add(prev);
            prev = prev.getPreviousBuildInProgress();
        }

        return runs;
    }

    /**
     * @return total tests executed
     */
    public int getTestCount()
    {
        Run<?, ?> run = this.job.getLastSuccessfulBuild();
        if (run != null)
        {
            AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
            return tests != null ? tests.getTotalCount() : 0;
        }
        return 0;
    }

    /**
     * @return total failed tests
     */
    public int getFailCount()
    {
        Run<?, ?> run = this.job.getLastSuccessfulBuild();
        if (run != null)
        {
            AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
            return tests != null ? tests.getFailCount() : 0;
        }
        return 0;
    }

    /**
     * @return total successful tests
     */
    public int getSuccessCount()
    {
        return this.getTestCount() - this.getFailCount();
    }

    /**
     * @return difference between this job's last build successful tests and the
     *         previous'
     */
    public String getDiff()
    {
        Run<?, ?> run = this.job.getLastSuccessfulBuild();
        if (run != null)
        {
            Run<?, ?> previous = this.getLastSuccessfulFrom(run);
            if (previous != null)
            {
                AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
                AbstractTestResultAction<?> prevTests = previous
                        .getAction(AbstractTestResultAction.class);
                if (tests != null && prevTests != null)
                {
                    int currentSuccess = tests.getTotalCount() - tests.getFailCount();
                    int prevSuccess = prevTests.getTotalCount() - prevTests.getFailCount();
                    return Functions.getDiffString(currentSuccess - prevSuccess);
                }
            }
        }
        return "";
    }

    /**
     * @param run
     *            a run
     * @return the last successful run prior to the given run
     */
    private Run<?, ?> getLastSuccessfulFrom(Run<?, ?> run)
    {
        Run<?, ?> r = run.getPreviousBuild();
        while (r != null
                && (r.isBuilding() || r.getResult() == null || r.getResult().isWorseThan(
                        Result.UNSTABLE)))
        {
            r = r.getPreviousBuild();
        }
        return r;
    }

    /**
     * Elects a culprit/responsible for a broken build by choosing the last
     * commiter of a given build
     * 
     * @return the culprit/responsible
     */
    public String getCulprit()
    {
        Run<?, ?> run = this.job.getLastBuild();
        String culprit = " - ";
        if (run instanceof AbstractBuild<?, ?>)
        {
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
            Iterator<User> it = build.getCulprits().iterator();
            while (it.hasNext())
            {
                culprit = it.next().getFullName();
            }
        }
        return culprit;
    }

    /**
     * @return color to be used to show the test diff
     */
    public String getDiffColor()
    {
        String diff = this.getDiff().trim();
        if (diff.length() > 0 && !Functions.getDiffString(0).equals(diff))
        {
            if (diff.startsWith("-"))
            {
                return "#FF0000";
            }
            else
            {
                return "#00FF00";
            }
        }
        return "#FFFFFF";
    }

    /**
     * @return the percentage of successful tests versus the total
     */
    public String getSuccessPercentage()
    {
        if (this.getTestCount() > 0)
        {
            Double perc = this.getSuccessCount() / (this.getTestCount() * 1D);
            return NumberFormat.getPercentInstance().format(perc);
        }
        return "";
    }

    /**
     * Determines some information of the current job like which colors use,
     * wether it's building or not or broken.
     */
    private void findStatus()
    {
        Result result = radiatorView.getResult(job);

        if (result.ordinal == Result.NOT_BUILT.ordinal)
        {
            this.backgroundColor = getColors().getOtherBG();
            this.color = getColors().getOtherFG();
        }
        else if (result.ordinal == Result.SUCCESS.ordinal)
        {
            this.backgroundColor = getColors().getOkBG();
            this.color = getColors().getOkFG();
            this.broken = false;
        }
        else if (result.ordinal == Result.UNSTABLE.ordinal)
        {
            this.backgroundColor = getColors().getFailedBG();
            this.color = getColors().getFailedFG();
            this.broken = false;
        }
        else
        {
            this.backgroundColor = getColors().getBrokenBG();
            this.color = getColors().getBrokenFG();
            this.broken = true;
        }

        switch (this.job.getIconColor())
        {
        case BLUE_ANIME:
        case YELLOW_ANIME:
        case RED_ANIME:
        case GREY_ANIME:
        case DISABLED_ANIME:
            this.building = true;
        default:
            this.building = false;
        }
    }

    private ViewEntryColors getColors()
    {
        return radiatorView.getColors();
    }
}