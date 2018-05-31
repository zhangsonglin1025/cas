package org.apereo.cas.web.flow;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.authentication.DefaultAuthenticationServiceSelectionPlan;
import org.apereo.cas.authentication.DefaultAuthenticationServiceSelectionStrategy;
import org.apereo.cas.authentication.principal.WebApplicationService;
import org.apereo.cas.services.DefaultRegisteredServiceAccessStrategy;
import org.apereo.cas.services.RegexRegisteredService;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.services.UnauthorizedServiceException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.webflow.test.MockRequestContext;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Mockito based tests for @{link ServiceAuthorizationCheck}
 *
 * @author Dmitriy Kopylenko
 * @since 3.5.0
 */
@Slf4j
public class ServiceAuthorizationCheckTests {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ServiceAuthorizationCheck serviceAuthorizationCheck;

    private final WebApplicationService authorizedService = mock(WebApplicationService.class);

    private final WebApplicationService unauthorizedService = mock(WebApplicationService.class);

    private final WebApplicationService undefinedService = mock(WebApplicationService.class);

    private final ServicesManager servicesManager = mock(ServicesManager.class);

    @Before
    public void setUpMocks() {
        final var authorizedRegisteredService = new RegexRegisteredService();
        final var unauthorizedRegisteredService = new RegexRegisteredService();
        unauthorizedRegisteredService.setAccessStrategy(new DefaultRegisteredServiceAccessStrategy(false, false));

        final List<RegisteredService> list = new ArrayList<>();
        list.add(authorizedRegisteredService);
        list.add(unauthorizedRegisteredService);
        
        when(this.servicesManager.findServiceBy(this.authorizedService)).thenReturn(authorizedRegisteredService);
        when(this.servicesManager.findServiceBy(this.unauthorizedService)).thenReturn(unauthorizedRegisteredService);
        when(this.servicesManager.findServiceBy(this.undefinedService)).thenReturn(null);
        when(this.servicesManager.getAllServices()).thenReturn(list);
        
        this.serviceAuthorizationCheck = new ServiceAuthorizationCheck(this.servicesManager, 
                new DefaultAuthenticationServiceSelectionPlan(new DefaultAuthenticationServiceSelectionStrategy()));
    }

    @Test
    public void noServiceProvided() {
        final var mockRequestContext = new MockRequestContext();
        final var event = this.serviceAuthorizationCheck.doExecute(mockRequestContext);
        assertEquals("success", event.getId());
    }

    @Test
    public void authorizedServiceProvided() {
        final var mockRequestContext = new MockRequestContext();
        mockRequestContext.getFlowScope().put("service", this.authorizedService);
        final var event = this.serviceAuthorizationCheck.doExecute(mockRequestContext);
        assertEquals("success", event.getId());
    }

    @Test
    public void unauthorizedServiceProvided() {
        final var mockRequestContext = new MockRequestContext();
        mockRequestContext.getFlowScope().put("service", this.unauthorizedService);

        this.thrown.expect(UnauthorizedServiceException.class);


        this.serviceAuthorizationCheck.doExecute(mockRequestContext);
        fail("Should have thrown UnauthorizedServiceException");
    }

    @Test
    public void serviceThatIsNotRegisteredProvided() {
        final var mockRequestContext = new MockRequestContext();
        mockRequestContext.getFlowScope().put("service", this.undefinedService);

        this.thrown.expect(UnauthorizedServiceException.class);


        this.serviceAuthorizationCheck.doExecute(mockRequestContext);
        throw new AssertionError("Should have thrown UnauthorizedServiceException");
    }
}
