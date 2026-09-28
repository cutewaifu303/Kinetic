

















package fr.litarvan.openauth.microsoft;

public class AuthTokens
{
    private final String accessToken;
    private final String refreshToken;

    public AuthTokens(String accessToken, String refreshToken)
    {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken()
    {
        return accessToken;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }
}
