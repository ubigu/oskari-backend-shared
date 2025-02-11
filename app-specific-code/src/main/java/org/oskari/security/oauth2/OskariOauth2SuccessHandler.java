package org.oskari.security.oauth2;

import fi.nls.oskari.domain.Role;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.UserService;
import fi.nls.oskari.user.DatabaseUserService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Profile("oauth2")
@Component
public class OskariOauth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final Logger logger = LogFactory.getLogger(OskariOauth2SuccessHandler.class);
    private static final int SESSION_INACTIVE_INTERVAL = 10*60*60; // 10h

    private final DatabaseUserService userService;
    private final OAuth2AuthorizedClientService clientService;
    private final ClientRegistration clientRegistration;
    private final EntraIDGraphApiClient entraApiClient;

    @Autowired
    public OskariOauth2SuccessHandler(
            OAuth2AuthorizedClientService clientService,
            ClientRegistration clientRegistration,
            EntraIDGraphApiClient entraApiClient
    ) {
        this.userService = getUserService();
        this.clientService = clientService;
        this.clientRegistration = clientRegistration;
        this.entraApiClient = entraApiClient;
    }

    private static final String ENTRA_ROLE_PREFIX = "ENTRA_ROLE_";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        final Object principal = authentication.getPrincipal();
        if (!(principal instanceof DefaultOidcUser oidcUser)) {
            throw new IllegalArgumentException("Expected DefaultOidcUser, got: " + principal.getClass().getName());
        }
        logger.warn("Received oidc user" + oidcUser);
        logger.warn("Received oidc user with claims" + oidcUser.getClaims());
        User user = null;
        try {
            user = getUser(oidcUser);
            OAuth2AuthorizedClient authorizedClient = clientService.loadAuthorizedClient(clientRegistration.getRegistrationId(), oidcUser.getName());
            logger.warn("Received authorized client: " + authorizedClient);

            // add default role for logged in user
            user.addRole(Role.getDefaultUserRole());

            if (user.getId() == -1) {
                userService.saveUser(user);
                // the user returned by createUser() doesn't have roles so find the user with email
                user = userService.getUserByEmail(user.getEmail());
            } else {
                userService.saveUser(user);
            }

        } catch (ServiceException | RuntimeException e) {
            throw new AuthenticationServiceException("Unable to load/store Oskari user data from/to PostgreSQL", e);
        }

        setupSession(user, request);

        super.onAuthenticationSuccess(request, response, authentication);
    }

    private User getUser(DefaultOidcUser oud) throws ServiceException {
        User user = userService.getUserByEmail(oud.getEmail());
        // sdf is not threadsafe so create new for each login
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        if (user == null) {
            user = new User();
            user.setAttribute("created", format.format(new Date()));
        }
        // add default role for logged in user
        user.addRole(Role.getDefaultUserRole());

        // Update user info
        copyInfoToUser(user, oud);
        user.setAttribute("lastLogin", format.format(new Date()));
        user.setLastLogin(OffsetDateTime.now());
        return user;

    }

    protected void setupSession(User authenticatedUser, HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        synchronized (session) {
            session.setAttribute(User.class.getName(), authenticatedUser);
            // Invalidate session after 10 hours, ie one workday
            session.setMaxInactiveInterval(SESSION_INACTIVE_INTERVAL);
        }
    }

    protected DatabaseUserService getUserService() {
        try {
            // throws class cast exception if configured other than intended
            return (DatabaseUserService) UserService.getInstance();
        } catch (ServiceException e) {
            throw new RuntimeException("Error getting UserService. Is it configured?", e);
        }
    }

    private User copyInfoToUser(User user, DefaultOidcUser oidcUser) {
        user.setEmail(oidcUser.getEmail().toLowerCase());
        // If user has no given and family names set, it is still possible that they
        // have full name. Fallback to use it as firstname
        if ((oidcUser.getGivenName() == null || oidcUser.getGivenName().isBlank()) &&
                ((oidcUser.getFamilyName()) == null || oidcUser.getFamilyName().isBlank())) {
            user.setFirstname(oidcUser.getFullName());
        } else {
            user.setFirstname(oidcUser.getGivenName());
            user.setLastname(oidcUser.getFamilyName());
        }
        user.setScreenname(oidcUser.getPreferredUsername());
        return user;
    }
}