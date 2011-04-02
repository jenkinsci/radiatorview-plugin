package hudson.model;

import hudson.Functions;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.plugins.claim.ClaimBuildAction;
import hudson.tasks.test.AbstractTestResultAction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;

/**
 * Represents a job to be shown in a view. Based heavily on the XFPanelEntry in
 * XFPanel plugin.
 * 
 * @author jrenaut
 */
public final class ViewEntry {

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
     * If the build is stable.
     */
    private boolean stable;

    /**
     * C'tor
     * 
     * @param job
     *            the job to be represented
     * @param radiatorView
     *            TODO
     */
    public ViewEntry(RadiatorView radiatorView, Job<?, ?> job) {
        this.radiatorView = radiatorView;
        this.job = job;
        this.findStatus();
    }

    /**
     * @return the job
     */
    public Job<?, ?> getJob() {
        return this.job;
    }

    /**
     * @return the job's name
     */
    public String getName() {
        return job.getName();
    }

    /**
     * @return if this job is queued for build
     */
    public Boolean getQueued() {
        return this.job.isInQueue();
    }

    /**
     * @return the job's queue number, if any
     */
    public Integer getQueueNumber() {
        return this.radiatorView.placeInQueue.get(this.job.getQueueItem());
    }

    /**
     * @return background color for this job
     */
    public String getBackgroundColor() {
        return this.backgroundColor;
    }

    /**
     * @return foreground color for this job
     */
    public String getColor() {
        return this.color;
    }

    /**
     * @return true se o último build está quebrado
     */
    public Boolean getBroken() {
        return this.broken;
    }

    /**
     * @return true if this job is currently being built
     */
    public Boolean getBuilding() {
        return this.building;
    }

    /**
     * @return the URL for the last build
     */
    public String getUrl() {
        return this.job.getUrl() + "lastBuild";
    }

    /**
     * @return a list will all the currently building runs for this job.
     */
    public List<Run<?, ?>> getBuildsInProgress() {
        List<Run<?, ?>> runs = new ArrayList<Run<?, ?>>();

        Run<?, ?> run = this.job.getLastBuild();
        if (run != null) {
            if (run.isBuilding()) {
                runs.add(run);
            }

            Run<?, ?> prev = run.getPreviousBuildInProgress();
            while (prev != null) {
                runs.add(prev);
                prev = prev.getPreviousBuildInProgress();
            }
        }

        return runs;
    }

    /**
     * @return total tests executed
     */
    public int getTestCount() {
        Run<?, ?> run = this.job.getLastSuccessfulBuild();
        if (run != null) {
            AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
            return tests != null ? tests.getTotalCount() : 0;
        }
        return 0;
    }

    /**
     * @return total failed tests
     */
    public int getFailCount() {
        Run<?, ?> run = this.job.getLastSuccessfulBuild();
        if (run != null) {
            AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
            return tests != null ? tests.getFailCount() : 0;
        }
        return 0;
    }

    /**
     * @return total successful tests
     */
    public int getSuccessCount() {
        return this.getTestCount() - this.getFailCount();
    }

    /**
     * @return difference between this job's last build successful tests and the
     *         previous'
     */
    public String getDiff() {
        Run<?, ?> run = this.job.getLastSuccessfulBuild();
        if (run != null) {
            Run<?, ?> previous = this.getLastSuccessfulFrom(run);
            if (previous != null) {
                AbstractTestResultAction<?> tests = run.getAction(AbstractTestResultAction.class);
                AbstractTestResultAction<?> prevTests = previous.getAction(AbstractTestResultAction.class);
                if (tests != null && prevTests != null) {
                    int currentSuccess = tests.getTotalCount() - tests.getFailCount();
                    int prevSuccess = prevTests.getTotalCount() - prevTests.getFailCount();
                    int diff = currentSuccess - prevSuccess;
                    if (diff != 0) {
                        return Functions.getDiffString(diff);
                    } else {
                        return "";
                    }
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
    private Run<?, ?> getLastSuccessfulFrom(Run<?, ?> run) {
        Run<?, ?> r = run.getPreviousBuild();
        while (r != null
                && (r.isBuilding() || r.getResult() == null || r.getResult().isWorseThan(
                Result.UNSTABLE))) {
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
    public String getCulprit() {
        Run<?, ?> run = this.job.getLastBuild();
        String culprit = " - ";
        Set<String> culprits = new HashSet<String>();
        while (run != null) {
            if (run instanceof AbstractBuild<?, ?>) {

                AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;

                Iterator<User> it = build.getCulprits().iterator();
                while (it.hasNext()) {
                    culprits.add(it.next().getFullName());
                }
            }
            run = run.getPreviousBuild();
            if (run != null && Result.SUCCESS.equals(run.getResult())) {
                // don't look for culprits in successful builds.
                run = null;
            }
        }
        if (!culprits.isEmpty()) {
            culprit = StringUtils.join(culprits, ", ");
        }
        return culprit;
    }

    /**
     * @return color to be used to show the test diff
     */
    public String getDiffColor() {
        String diff = this.getDiff().trim();
        if (diff.length() > 0 && !Functions.getDiffString(0).equals(diff)) {
            if (diff.startsWith("-")) {
                return "#FF0000";
            } else {
                return "#00FF00";
            }
        }
        return "#FFFFFF";
    }

    /**
     * @return the percentage of successful tests versus the total
     */
    public String getSuccessPercentage() {
        if (this.getTestCount() > 0) {
            Double perc = this.getSuccessCount() / (this.getTestCount() * 1D);
            return NumberFormat.getPercentInstance().format(perc);
        }
        return "";
    }

    /**
     * Determines some information of the current job like which colors use,
     * wether it's building or not or broken.
     */
    private void findStatus() {
        Result result = radiatorView.getResult(job);

        this.stable = false;
        if (result.ordinal == Result.NOT_BUILT.ordinal) {
            this.backgroundColor = getColors().getOtherBG();
            this.color = getColors().getOtherFG();
        } else if (result.ordinal == Result.SUCCESS.ordinal) {
            this.backgroundColor = getColors().getOkBG();
            this.color = getColors().getOkFG();
            this.broken = false;
            this.stable = true;
        } else if (result.ordinal == Result.UNSTABLE.ordinal) {
            this.backgroundColor = getColors().getFailedBG();
            this.color = getColors().getFailedFG();
            this.broken = false;
        } else {
            this.backgroundColor = getColors().getBrokenBG();
            this.color = getColors().getBrokenFG();
            this.broken = true;
        }

        switch (this.job.getIconColor()) {
            case BLUE_ANIME:
            case YELLOW_ANIME:
            case RED_ANIME:
            case GREY_ANIME:
            case DISABLED_ANIME:
                this.building = true;
                break;
            default:
                this.building = false;
        }
    }

    private ViewEntryColors getColors() {
        return radiatorView.getColors();
    }

    public String getLastCompletedBuild() {
        Run build = job.getLastCompletedBuild();
        if (build != null) {
            return build.getTimestampString() + " (" + build.getDurationString() + ")";
        }
        return null;
    }

    public String getLastStableBuild() {
        Run build = job.getLastStableBuild();
        if (build != null) {
            return build.getTimestampString() + " (in " + build.getDurationString() + ")";
        }
        return null;
    }

    /**
     * @return <code>true</code> if the build is stable.
     */
    public boolean getStable() {
        return stable;
    }

    /**
     * If the claims plugin is installed, this will get details of the claimed
     * build failures.
     * 
     * @return details of any claims for the broken build, or null if nobody has
     *         claimed this build.
     */
    public String getClaim() {
        
        // check we have claim plugin
        if (Hudson.getInstance().getPlugin("claim") == null) {
            return null;
        }

        

        Run lastBuild = job.getLastBuild();
        if (lastBuild != null && lastBuild.isBuilding()) {
            // claims can only be made against builds once they've finished,
            // so check the previous build if currently building.
            lastBuild = lastBuild.getPreviousBuild();
        }        
        if (lastBuild == null) {
            return null;
        }

        // find the claim 
        String result ="";
        if (lastBuild instanceof hudson.matrix.MatrixBuild) {
            MatrixBuild matrixBuild = (hudson.matrix.MatrixBuild) lastBuild;
            
            for (MatrixRun combination : matrixBuild.getRuns()) {
                ClaimBuildAction claimAction = getClaimForRun(combination);
                if (claimAction != null && claimAction.isClaimed()) {
                    result += combination.getParent().getCombination().toString() + ": "+ claimAction.getReason() + " (" + claimAction.getClaimedByName() + ").<br />";
                } else {
                    result += combination.getParent().getCombination().toString() + ": Not Claimed.<br />";
                }                
            }
        } else {
            ClaimBuildAction claimAction = getClaimForRun(lastBuild);
            if (claimAction != null && claimAction.isClaimed()) {
                result = claimAction.getReason() + " (" + claimAction.getClaimedByName() + ").";
            } else {
                result = "Not Claimed.";
            }
        }
        return result;
    }

    private ClaimBuildAction getClaimForRun(Run run) {
        ClaimBuildAction claimAction = null;
        List<ClaimBuildAction> claimActionList = run.getActions(ClaimBuildAction.class);
        if (claimActionList.size() == 1) {
            claimAction = claimActionList.get(0);
        } else if (claimActionList.size() > 1) {            
            Log.warn("Multiple ClaimBuildActions found for job " + job.toString());
        }
        return claimAction;
    }
}