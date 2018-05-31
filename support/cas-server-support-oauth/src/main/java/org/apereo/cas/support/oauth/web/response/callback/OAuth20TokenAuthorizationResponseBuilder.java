package org.apereo.cas.support.oauth.web.response.callback;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.web.response.accesstoken.OAuth20TokenGenerator;
import org.apereo.cas.support.oauth.web.response.accesstoken.ext.AccessTokenRequestDataHolder;
import org.apereo.cas.ticket.ExpirationPolicy;
import org.apereo.cas.ticket.accesstoken.AccessToken;
import org.apereo.cas.ticket.refreshtoken.RefreshToken;
import org.apereo.cas.util.EncodingUtils;
import org.pac4j.core.context.J2EContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import java.util.ArrayList;
import java.util.List;

/**
 * This is {@link OAuth20TokenAuthorizationResponseBuilder}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
@AllArgsConstructor
public class OAuth20TokenAuthorizationResponseBuilder implements OAuth20AuthorizationResponseBuilder {

    private final OAuth20TokenGenerator accessTokenGenerator;
    private final ExpirationPolicy accessTokenExpirationPolicy;

    @Override
    @SneakyThrows
    public View build(final J2EContext context, final String clientId, final AccessTokenRequestDataHolder holder) {

        final var redirectUri = context.getRequestParameter(OAuth20Constants.REDIRECT_URI);
        LOGGER.debug("Authorize request verification successful for client [{}] with redirect uri [{}]", clientId, redirectUri);
        final var accessToken = accessTokenGenerator.generate(holder);
        final var key = accessToken.getKey();
        LOGGER.debug("Generated OAuth access token: [{}]", key);
        return buildCallbackUrlResponseType(holder, redirectUri, key, new ArrayList<>(), accessToken.getValue(), context);

    }


    /**
     * Build callback url response type string.
     *
     * @param holder       the holder
     * @param redirectUri  the redirect uri
     * @param accessToken  the access token
     * @param params       the params
     * @param refreshToken the refresh token
     * @param context      the context
     * @return the string
     * @throws Exception the exception
     */
    protected View buildCallbackUrlResponseType(final AccessTokenRequestDataHolder holder,
                                                final String redirectUri,
                                                final AccessToken accessToken,
                                                final List<NameValuePair> params,
                                                final RefreshToken refreshToken,
                                                final J2EContext context) throws Exception {
        final var state = holder.getAuthentication().getAttributes().get(OAuth20Constants.STATE).toString();
        final var nonce = holder.getAuthentication().getAttributes().get(OAuth20Constants.NONCE).toString();

        final var builder = new URIBuilder(redirectUri);
        final var stringBuilder = new StringBuilder();
        stringBuilder.append(OAuth20Constants.ACCESS_TOKEN)
            .append('=')
            .append(accessToken.getId())
            .append('&')
            .append(OAuth20Constants.TOKEN_TYPE)
            .append('=')
            .append(OAuth20Constants.TOKEN_TYPE_BEARER)
            .append('&')
            .append(OAuth20Constants.EXPIRES_IN)
            .append('=')
            .append(accessTokenExpirationPolicy.getTimeToLive());

        if (refreshToken != null) {
            stringBuilder.append('&')
                .append(OAuth20Constants.REFRESH_TOKEN)
                .append('=')
                .append(refreshToken.getId());
        }

        params.forEach(p -> stringBuilder.append('&')
            .append(p.getName())
            .append('=')
            .append(p.getValue()));

        if (StringUtils.isNotBlank(state)) {
            stringBuilder.append('&')
                .append(OAuth20Constants.STATE)
                .append('=')
                .append(EncodingUtils.urlEncode(state));
        }
        if (StringUtils.isNotBlank(nonce)) {
            stringBuilder.append('&')
                .append(OAuth20Constants.NONCE)
                .append('=')
                .append(EncodingUtils.urlEncode(nonce));
        }
        builder.setFragment(stringBuilder.toString());
        final var url = builder.toString();

        LOGGER.debug("Redirecting to URL [{}]", url);
        return new RedirectView(url);
    }

    @Override
    public boolean supports(final J2EContext context) {
        final var responseType = context.getRequestParameter(OAuth20Constants.RESPONSE_TYPE);
        return StringUtils.equalsIgnoreCase(responseType, OAuth20ResponseTypes.TOKEN.getType());
    }
}
