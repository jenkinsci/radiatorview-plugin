package hudson.model;

/**
 * Represents colors to be used on the view
 * 
 * @author jrenaut
 */
public final class ViewEntryColors
{

    private String okBG;
    private String okFG;
    private String failedBG;
    private String failedFG;
    private String brokenBG;
    private String brokenFG;
    private String otherBG;
    private String otherFG;

    /**
     * C'tor
     * 
     * @param okBG
     *            ok builds background color
     * @param okFG
     *            ok builds foreground color
     * @param failedBG
     *            failed build background color
     * @param failedFG
     *            failed build foreground color
     * @param brokenBG
     *            broken build background color
     * @param brokenFG
     *            broken build foreground color
     * @param otherBG
     *            other build background color
     * @param otherFG
     *            other build foreground color
     */
    public ViewEntryColors(String okBG, String okFG, String failedBG, String failedFG,
            String brokenBG, String brokenFG, String otherBG, String otherFG)
    {
        super();
        this.okBG = okBG;
        this.okFG = okFG;
        this.failedBG = failedBG;
        this.failedFG = failedFG;
        this.brokenBG = brokenBG;
        this.brokenFG = brokenFG;
        this.otherBG = otherBG;
        this.otherFG = otherFG;
    }

    /**
     * @return the okBG
     */
    public String getOkBG()
    {
        return okBG;
    }

    /**
     * @return the okFG
     */
    public String getOkFG()
    {
        return okFG;
    }

    /**
     * @return the failedBG
     */
    public String getFailedBG()
    {
        return failedBG;
    }

    /**
     * @return the failedFG
     */
    public String getFailedFG()
    {
        return failedFG;
    }

    /**
     * @return the brokenBG
     */
    public String getBrokenBG()
    {
        return brokenBG;
    }

    /**
     * @return the brokenFG
     */
    public String getBrokenFG()
    {
        return brokenFG;
    }

    /**
     * @return the otherBG
     */
    public String getOtherBG()
    {
        return otherBG;
    }

    /**
     * @return the otherFG
     */
    public String getOtherFG()
    {
        return otherFG;
    }

    public static final ViewEntryColors DEFAULT = new ViewEntryColors("#88ff88", "black", "yellow", "black",
            "red", "white", "#CCCCCC", "#FFFFFF");
}