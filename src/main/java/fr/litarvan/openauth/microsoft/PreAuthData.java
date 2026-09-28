

















package fr.litarvan.openauth.microsoft;

public class PreAuthData
{
    private final String ppft;
    private final String urlPost;

    public PreAuthData(String ppft, String urlPost)
    {
        this.ppft = ppft;
        this.urlPost = urlPost;
    }

    public String getPPFT()
    {
        return ppft;
    }

    public String getUrlPost()
    {
        return urlPost;
    }
}
