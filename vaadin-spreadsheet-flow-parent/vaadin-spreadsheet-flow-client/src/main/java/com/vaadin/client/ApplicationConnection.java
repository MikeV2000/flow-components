/**
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>} for the full
 * license.
 */
package com.vaadin.client;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gwt.aria.client.LiveValue;
import com.google.gwt.aria.client.RelevantValue;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.addon.spreadsheet.client.SpreadsheetOverlay;
import com.vaadin.client.ApplicationConfiguration.ErrorMessage;
import com.vaadin.client.communication.ConnectionStateHandler;
import com.vaadin.client.communication.Heartbeat;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.MessageSender;
import com.vaadin.client.communication.RpcManager;
import com.vaadin.client.communication.ServerRpcQueue;
import com.vaadin.client.metadata.ConnectorBundleLoader;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.client.ui.AbstractConnector;
import com.vaadin.client.ui.Icon;
import com.vaadin.client.ui.VContextMenu;
import com.vaadin.client.ui.VNotification;
import com.vaadin.client.ui.VOverlay;
import com.vaadin.client.ui.ui.UIConnector;
import com.vaadin.shared.Version;
import com.vaadin.shared.util.SharedUtil;

/**
 * This is the client side communication "engine", managing client-server
 * communication with its server side counterpart
 * com.vaadin.server.VaadinService.
 *
 * Client-side connectors receive updates from the corresponding server-side
 * connector (typically component) as state updates or RPC calls. The connector
 * has the possibility to communicate back with its server side counter part
 * through RPC calls.
 *
 * TODO document better
 *
 * Entry point classes (widgetsets) define <code>onModuleLoad()</code>.
 */
public class ApplicationConnection implements HasHandlers {

    @Deprecated
    public static final String MODIFIED_CLASSNAME = StyleConstants.MODIFIED;

    @Deprecated
    public static final String DISABLED_CLASSNAME = StyleConstants.DISABLED;

    @Deprecated
    public static final String REQUIRED_CLASSNAME = StyleConstants.REQUIRED;

    @Deprecated
    public static final String REQUIRED_CLASSNAME_EXT = StyleConstants.REQUIRED_EXT;

    @Deprecated
    public static final String ERROR_CLASSNAME_EXT = StyleConstants.ERROR_EXT;

    /**
     * A string that, if found in a non-JSON response to a UIDL request, will
     * cause the browser to refresh the page. If followed by a colon, optional
     * whitespace, and a URI, causes the browser to synchronously load the URI.
     *
     * <p>
     * This allows, for instance, a servlet filter to redirect the application
     * to a custom login page when the session expires. For example:
     * </p>
     *
     * <pre>
     * if (sessionExpired) {
     *     response.setHeader(&quot;Content-Type&quot;, &quot;text/html&quot;);
     *     response.getWriter().write(myLoginPageHtml + &quot;&lt;!-- Vaadin-Refresh: &quot;
     *             + request.getContextPath() + &quot; --&gt;&quot;);
     * }
     * </pre>
     */
    public static final String UIDL_REFRESH_TOKEN = "Vaadin-Refresh";

    private final Map<String, String> resourcesMap = new HashMap<>();

    private WidgetSet widgetSet;

    private VContextMenu contextMenu = null;

    private final UIConnector uIConnector;

    protected boolean cssLoaded = false;

    /** Parameters for this application connection loaded from the web-page */
    private ApplicationConfiguration configuration;

    public enum ApplicationState {
        INITIALIZING, RUNNING, TERMINATED;
    }

    private ApplicationState applicationState = ApplicationState.INITIALIZING;

    /**
     * The communication handler methods are called at certain points during
     * communication with the server. This allows for making add-ons that keep
     * track of different aspects of the communication.
     */
    public interface CommunicationHandler extends EventHandler {
        void onRequestStarting(RequestStartingEvent e);

        void onResponseHandlingStarted(ResponseHandlingStartedEvent e);

        void onResponseHandlingEnded(ResponseHandlingEndedEvent e);
    }

    public static class RequestStartingEvent
            extends ApplicationConnectionEvent {

        public static Type<CommunicationHandler> TYPE = new Type<>();

        public RequestStartingEvent(ApplicationConnection connection) {
            super(connection);
        }

        @Override
        public Type<CommunicationHandler> getAssociatedType() {
            return TYPE;
        }

        @Override
        protected void dispatch(CommunicationHandler handler) {
            handler.onRequestStarting(this);
        }
    }

    public static class ResponseHandlingEndedEvent
            extends ApplicationConnectionEvent {

        public static Type<CommunicationHandler> TYPE = new Type<>();

        public ResponseHandlingEndedEvent(ApplicationConnection connection) {
            super(connection);
        }

        @Override
        public Type<CommunicationHandler> getAssociatedType() {
            return TYPE;
        }

        @Override
        protected void dispatch(CommunicationHandler handler) {
            handler.onResponseHandlingEnded(this);
        }
    }

    public abstract static class ApplicationConnectionEvent
            extends GwtEvent<CommunicationHandler> {

        private ApplicationConnection connection;

        protected ApplicationConnectionEvent(ApplicationConnection connection) {
            this.connection = connection;
        }

        public ApplicationConnection getConnection() {
            return connection;
        }

    }

    public static class ResponseHandlingStartedEvent
            extends ApplicationConnectionEvent {

        public ResponseHandlingStartedEvent(ApplicationConnection connection) {
            super(connection);
        }

        public static Type<CommunicationHandler> TYPE = new Type<>();

        @Override
        public Type<CommunicationHandler> getAssociatedType() {
            return TYPE;
        }

        @Override
        protected void dispatch(CommunicationHandler handler) {
            handler.onResponseHandlingStarted(this);
        }
    }

    /**
     * Event triggered when a application is stopped by calling
     * {@link ApplicationConnection#setApplicationRunning(boolean)}.
     *
     * To listen for the event add a {@link ApplicationStoppedHandler} by
     * invoking {@link ApplicationConnection#addHandler(Type, EventHandler)} to
     * the {@link ApplicationConnection}
     *
     * @since 7.1.8
     * @author Vaadin Ltd
     */
    public static class ApplicationStoppedEvent
            extends GwtEvent<ApplicationStoppedHandler> {

        public static Type<ApplicationStoppedHandler> TYPE = new Type<>();

        @Override
        public Type<ApplicationStoppedHandler> getAssociatedType() {
            return TYPE;
        }

        @Override
        protected void dispatch(ApplicationStoppedHandler listener) {
            listener.onApplicationStopped(this);
        }
    }

    /**
     * Allows custom handling of communication errors.
     */
    public interface CommunicationErrorHandler {
        /**
         * Called when a communication error has occurred. Returning
         * <code>true</code> from this method suppresses error handling.
         *
         * @param details
         *            A string describing the error.
         * @param statusCode
         *            The HTTP status code (e.g. 404, etc).
         * @return true if the error reporting should be suppressed, false to
         *         perform normal error reporting.
         */
        public boolean onError(String details, int statusCode);
    }

    /**
     * A listener for listening to application stopped events. The listener can
     * be added to a {@link ApplicationConnection} by invoking
     * {@link ApplicationConnection#addHandler(GwtEvent.Type, EventHandler)}
     *
     * @since 7.1.8
     * @author Vaadin Ltd
     */
    public interface ApplicationStoppedHandler extends EventHandler {

        /**
         * Triggered when the {@link ApplicationConnection} marks a previously
         * running application as stopped by invoking
         * {@link ApplicationConnection#setApplicationRunning(boolean)}.
         *
         * @param event
         *            the event triggered by the {@link ApplicationConnection}
         */
        void onApplicationStopped(ApplicationStoppedEvent event);
    }

    private CommunicationErrorHandler communicationErrorDelegate = null;

    public static class MultiStepDuration extends Duration {
        private int previousStep = elapsedMillis();

        public void logDuration(String message) {
            logDuration(message, 0);
        }

        public void logDuration(String message, int minDuration) {
            int currentTime = elapsedMillis();
            int stepDuration = currentTime - previousStep;
            if (stepDuration >= minDuration) {
                getLogger().info(message + ": " + stepDuration + " ms");
            }
            previousStep = currentTime;
        }
    }

    public ApplicationConnection() {
        // Assuming UI data is eagerly loaded
        ConnectorBundleLoader.get()
                .loadBundle(ConnectorBundleLoader.EAGER_BUNDLE_NAME, null);
        uIConnector = GWT.create(UIConnector.class);
    }

    public void init(WidgetSet widgetSet, ApplicationConfiguration cnf) {
        getLogger().info("Starting application " + cnf.getRootPanelId());
        getLogger().info("Using theme: " + cnf.getThemeName());

        getLogger().info("Vaadin application servlet version: "
                + cnf.getServletVersion());

        if (!cnf.getServletVersion().equals(Version.getFullVersion())) {
            getLogger().severe(
                    "Warning: your widget set seems to be built with a different "
                            + "version than the one used on server. Unexpected "
                            + "behavior may occur.");
        }

        this.widgetSet = widgetSet;
        configuration = cnf;

        String appRootPanelName = cnf.getRootPanelId();
        // remove the end (window name) of autogenerated rootpanel id
        appRootPanelName = appRootPanelName.replaceFirst("-\\d+$", "");

        if (cnf.getRootElement() != null) {
            uIConnector.init(cnf.getRootElement(), this);
        } else {
            uIConnector.init(cnf.getRootPanelId(), this);
        }

        // Ensure the overlay container is added to the dom and set as a live
        // area for assistive devices
        Element overlayContainer = VOverlay.getOverlayContainer(this);
        Roles.getAlertRole().setAriaLiveProperty(overlayContainer,
                LiveValue.ASSERTIVE);
        VOverlay.setOverlayContainerLabel(this,
                getUIConnector().getState().overlayContainerLabel);
        Roles.getAlertRole().setAriaRelevantProperty(overlayContainer,
                RelevantValue.ADDITIONS);
    }

    /**
     * Starts this application. Don't call this method directly - it's called by
     * {@link ApplicationConfiguration#startApplication(String)}, which should
     * be called once this application has started (first response received) or
     * failed to start. This ensures that the applications are started in order,
     * to avoid session-id problems.
     *
     */
    public void start() {

    }

    /**
     * Requests an analyze of layouts, to find inconsistencies. Exclusively used
     * for debugging during development.
     *
     * @deprecated as of 7.1. Replaced by {@link UIConnector#analyzeLayouts()}
     */
    @Deprecated
    public void analyzeLayouts() {
        getUIConnector().analyzeLayouts();
    }

    /**
     * Sends a request to the server to print details to console that will help
     * the developer to locate the corresponding server-side connector in the
     * source code.
     *
     * @param serverConnector
     * @deprecated as of 7.1. Replaced by
     *             {@link UIConnector#showServerDebugInfo(ServerConnector)}
     */
    @Deprecated
    void highlightConnector(ServerConnector serverConnector) {
        getUIConnector().showServerDebugInfo(serverConnector);
    }

    int cssWaits = 0;

    static final int MAX_CSS_WAITS = 100;

    public void executeWhenCSSLoaded(final Command c) {
        if (!isCSSLoaded() && cssWaits < MAX_CSS_WAITS) {
            (new Timer() {
                @Override
                public void run() {
                    executeWhenCSSLoaded(c);
                }
            }).schedule(50);

            // Show this message just once
            if (cssWaits++ == 0) {
                getLogger().warning("Assuming CSS loading is not complete, "
                        + "postponing render phase. "
                        + "(.v-loading-indicator height == 0)");
            }
        } else {
            cssLoaded = true;
            if (cssWaits >= MAX_CSS_WAITS) {
                getLogger().severe("CSS files may have not loaded properly.");
            }

            c.execute();
        }
    }

    /**
     * Checks whether or not the CSS is loaded. By default checks the size of
     * the loading indicator element.
     *
     * @return
     */
    protected boolean isCSSLoaded() {
        return cssLoaded;
    }

    /**
     * Shows the communication error notification.
     *
     * @param details
     *            Optional details.
     * @param statusCode
     *            The status code returned for the request
     *
     */
    public void showCommunicationError(String details, int statusCode) {
        getLogger().severe("Communication error: " + details);
        showError(details, configuration.getCommunicationError());
    }

    /**
     * Shows the authentication error notification.
     *
     * @param details
     *            Optional details.
     */
    public void showAuthenticationError(String details) {
        getLogger().severe("Authentication error: " + details);
        showError(details, configuration.getAuthorizationError());
    }

    /**
     * Shows the session expiration notification.
     *
     * @param details
     *            Optional details.
     */
    public void showSessionExpiredError(String details) {
        getLogger().severe("Session expired: " + details);
        showError(details, configuration.getSessionExpiredError());
    }

    /**
     * Shows an error notification.
     *
     * @param details
     *            Optional details.
     * @param message
     *            An ErrorMessage describing the error.
     */
    protected void showError(String details, ErrorMessage message) {
        VNotification.showError(this, message.getCaption(),
                message.getMessage(), details, message.getUrl());
    }

    /**
     * Returns the loading indicator used by this ApplicationConnection.
     *
     * @return The loading indicator for this ApplicationConnection
     */
    public VLoadingIndicator getLoadingIndicator() {
        return null;
    }

    /**
     * Determines whether or not the loading indicator is showing.
     *
     * @return true if the loading indicator is visible
     * @deprecated As of 7.1. Use {@link #getLoadingIndicator()} and
     *             {@link VLoadingIndicator#isVisible()}.isVisible() instead.
     */
    @Deprecated
    public boolean isLoadingIndicatorVisible() {
        return false;
    }

    private void addVariableToQueue(String connectorId, String variableName,
            Object value, boolean immediate) {
    }

    /**
     * @deprecated as of 7.6, use {@link ServerRpcQueue#flush()}
     */
    @Deprecated
    public void sendPendingVariableChanges() {

    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * </p>
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param newValue
     *            the new value to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */
    public void updateVariable(String paintableId, String variableName,
            ServerConnector newValue, boolean immediate) {
        addVariableToQueue(paintableId, variableName, newValue, immediate);
    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * </p>
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param newValue
     *            the new value to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */

    public void updateVariable(String paintableId, String variableName,
            String newValue, boolean immediate) {
        addVariableToQueue(paintableId, variableName, newValue, immediate);
    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * </p>
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param newValue
     *            the new value to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */

    public void updateVariable(String paintableId, String variableName,
            int newValue, boolean immediate) {
        addVariableToQueue(paintableId, variableName, newValue, immediate);
    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * </p>
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param newValue
     *            the new value to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */

    public void updateVariable(String paintableId, String variableName,
            long newValue, boolean immediate) {
        addVariableToQueue(paintableId, variableName, newValue, immediate);
    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * </p>
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param newValue
     *            the new value to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */

    public void updateVariable(String paintableId, String variableName,
            float newValue, boolean immediate) {
        addVariableToQueue(paintableId, variableName, newValue, immediate);
    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * </p>
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param newValue
     *            the new value to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */

    public void updateVariable(String paintableId, String variableName,
            double newValue, boolean immediate) {
        addVariableToQueue(paintableId, variableName, newValue, immediate);
    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * </p>
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param newValue
     *            the new value to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */

    public void updateVariable(String paintableId, String variableName,
            boolean newValue, boolean immediate) {
        addVariableToQueue(paintableId, variableName, newValue, immediate);
    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * </p>
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param map
     *            the new values to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */
    public void updateVariable(String paintableId, String variableName,
            Map<String, Object> map, boolean immediate) {
        addVariableToQueue(paintableId, variableName, map, immediate);
    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * <p>
     * A null array is sent as an empty array.
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param values
     *            the new value to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */
    public void updateVariable(String paintableId, String variableName,
            String[] values, boolean immediate) {
        addVariableToQueue(paintableId, variableName, values, immediate);
    }

    /**
     * Sends a new value for the given paintables given variable to the server.
     * <p>
     * The update is actually queued to be sent at a suitable time. If immediate
     * is true, the update is sent as soon as possible. If immediate is false,
     * the update will be sent along with the next immediate update.
     * <p>
     * A null array is sent as an empty array.
     *
     * @param paintableId
     *            the id of the paintable that owns the variable
     * @param variableName
     *            the name of the variable
     * @param values
     *            the new value to be sent
     * @param immediate
     *            true if the update is to be sent as soon as possible
     */
    public void updateVariable(String paintableId, String variableName,
            Object[] values, boolean immediate) {
        addVariableToQueue(paintableId, variableName, values, immediate);
    }

    /**
     * Does absolutely nothing. Replaced by {@link LayoutManager}.
     *
     * @param container
     * @deprecated As of 7.0, serves no purpose
     */
    @Deprecated
    public void runDescendentsLayout(HasWidgets container) {
    }

    /**
     * This will cause re-layouting of all components. Mainly used for
     * development. Published to JavaScript.
     */
    public void forceLayout() {
        Duration duration = new Duration();

        getLogger().info("forceLayout in " + duration.elapsedMillis() + " ms");
    }

    /**
     * Returns false
     *
     * @param paintable
     * @return false, always
     * @deprecated As of 7.0, serves no purpose
     */
    @Deprecated
    private boolean handleComponentRelativeSize(ComponentConnector paintable) {
        return false;
    }

    /**
     * Returns false.
     *
     * @param widget
     * @return false, always
     * @deprecated As of 7.0, serves no purpose
     */
    @Deprecated
    public boolean handleComponentRelativeSize(Widget widget) {
        return handleComponentRelativeSize(connectorMap.getConnector(widget));

    }

    @Deprecated
    public ComponentConnector getPaintable(UIDL uidl) {
        // Non-component connectors shouldn't be painted from legacy connectors
        return (ComponentConnector) getConnector(uidl.getId(),
                Integer.parseInt(uidl.getTag()));
    }

    /**
     * Get either an existing ComponentConnector or create a new
     * ComponentConnector with the given type and id.
     *
     * If a ComponentConnector with the given id already exists, returns it.
     * Otherwise creates and registers a new ComponentConnector of the given
     * type.
     *
     * @param connectorId
     *            Id of the paintable
     * @param connectorType
     *            Type of the connector, as passed from the server side
     *
     * @return Either an existing ComponentConnector or a new ComponentConnector
     *         of the given type
     */
    public ServerConnector getConnector(String connectorId, int connectorType) {
        if (!connectorMap.hasConnector(connectorId)) {
            return createAndRegisterConnector(connectorId, connectorType);
        }
        return connectorMap.getConnector(connectorId);
    }

    /**
     * Creates a new ServerConnector with the given type and id.
     *
     * Creates and registers a new ServerConnector of the given type. Should
     * never be called with the connector id of an existing connector.
     *
     * @param connectorId
     *            Id of the new connector
     * @param connectorType
     *            Type of the connector, as passed from the server side
     *
     * @return A new ServerConnector of the given type
     */
    private ServerConnector createAndRegisterConnector(String connectorId,
            int connectorType) {
        Profiler.enter("ApplicationConnection.createAndRegisterConnector");

        // Create and register a new connector with the given type
        ServerConnector p = widgetSet.createConnector(connectorType,
                configuration);
        connectorMap.registerConnector(connectorId, p);
        p.doInit(connectorId, this);

        Profiler.leave("ApplicationConnection.createAndRegisterConnector");
        return p;
    }

    /**
     * Gets a resource that has been pre-loaded via UIDL, such as custom
     * layouts.
     *
     * @param name
     *            identifier of the resource to get
     * @return the resource
     */
    public String getResource(String name) {
        return resourcesMap.get(name);
    }

    /**
     * Sets a resource that has been pre-loaded via UIDL, such as custom
     * layouts.
     *
     * @since 7.6
     * @param name
     *            identifier of the resource to Set
     * @param resource
     *            the resource
     */
    public void setResource(String name, String resource) {
        resourcesMap.put(name, resource);
    }

    /**
     * Singleton method to get instance of app's context menu.
     *
     * @return VContextMenu object
     */
    public VContextMenu getContextMenu() {
        if (contextMenu == null) {
            contextMenu = new SpreadsheetOverlay.SpreadsheetContextMenu();
            contextMenu.setOwner(uIConnector.getWidget());
        }
        return contextMenu;
    }

    /**
     * Gets an {@link Icon} instance corresponding to a URI.
     *
     * @since 7.2
     * @param uri
     * @return Icon object
     */
    public Icon getIcon(String uri) {
        return null;
    }

    /**
     * Translates custom protocols in UIDL URI's to be recognizable by browser.
     * All uri's from UIDL should be routed via this method before giving them
     * to browser due URI's in UIDL may contain custom protocols like theme://.
     *
     * @param uidlUri
     *            Vaadin URI from uidl
     * @return translated URI ready for browser
     */
    public String translateVaadinUri(String uidlUri) {
        return null;
    }

    /**
     * Gets the URI for the current theme. Can be used to reference theme
     * resources.
     *
     * @return URI to the current theme
     */
    public String getThemeUri() {
        return configuration.getVaadinDirUrl() + "themes/"
                + getUIConnector().getActiveTheme();
    }

    private ConnectorMap connectorMap = GWT.create(ConnectorMap.class);

    /**
     * Use to notify that the given component's caption has changed; layouts may
     * have to be recalculated.
     *
     * @param widget
     *            The Widget whose caption has changed
     * @deprecated As of 7.0.2, has not had any effect for a long time
     */
    @Deprecated
    public void captionSizeUpdated(Widget widget) {
        // This doesn't do anything, it's just kept here for compatibility
    }

    /**
     * Gets the main view.
     *
     * @return the main view
     */
    public UIConnector getUIConnector() {
        return uIConnector;
    }

    /**
     * Gets the {@link ApplicationConfiguration} for the current application.
     *
     * @see ApplicationConfiguration
     * @return the configuration for this application
     */
    public ApplicationConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Checks if there is a registered server side listener for the event. The
     * list of events which has server side listeners is updated automatically
     * before the component is updated so the value is correct if called from
     * updatedFromUIDL.
     *
     * @param connector
     *            The connector to register event listeners for
     * @param eventIdentifier
     *            The identifier for the event
     * @return true if at least one listener has been registered on server side
     *         for the event identified by eventIdentifier.
     * @deprecated As of 7.0. Use
     *             {@link AbstractConnector#hasEventListener(String)} instead
     */
    @Deprecated
    public boolean hasEventListeners(ComponentConnector connector,
            String eventIdentifier) {
        return connector.hasEventListener(eventIdentifier);
    }

    /**
     * Adds the get parameters to the uri and returns the new uri that contains
     * the parameters.
     *
     * @param uri
     *            The uri to which the parameters should be added.
     * @param extraParams
     *            One or more parameters in the format "a=b" or "c=d&amp;e=f".
     *            An empty string is allowed but will not modify the url.
     * @return The modified URI with the get parameters in extraParams added.
     * @deprecated Use {@link SharedUtil#addGetParameters(String,String)}
     *             instead
     */
    @Deprecated
    public static String addGetParameters(String uri, String extraParams) {
        return SharedUtil.addGetParameters(uri, extraParams);
    }

    ConnectorMap getConnectorMap() {
        return connectorMap;
    }

    /**
     * @deprecated As of 7.0. No longer serves any purpose.
     */
    @Deprecated
    public void unregisterPaintable(ServerConnector p) {
        getLogger().info("unregisterPaintable (unnecessarily) called for "
                + Util.getConnectorString(p));
    }

    /**
     * Get VTooltip instance related to application connection.
     *
     * @return VTooltip instance
     */
    public VTooltip getVTooltip() {
        return null;
    }

    /**
     * Method provided for backwards compatibility. Duties previously done by
     * this method is now handled by the state change event handler in
     * AbstractComponentConnector. The only function this method has is to
     * return true if the UIDL is a "cached" update.
     *
     * @param component
     * @param uidl
     * @param manageCaption
     * @deprecated As of 7.0, no longer serves any purpose
     * @return
     */
    @Deprecated
    public boolean updateComponent(Widget component, UIDL uidl,
            boolean manageCaption) {
        ComponentConnector connector = getConnectorMap()
                .getConnector(component);
        if (!AbstractComponentConnector.isRealUpdate(uidl)) {
            return true;
        }

        if (!manageCaption) {
            getLogger().warning(Util.getConnectorString(connector)
                    + " called updateComponent with manageCaption=false. The parameter was ignored - override delegateCaption() to return false instead. It is however not recommended to use caption this way at all.");
        }
        return false;
    }

    /**
     * @deprecated As of 7.0. Use
     *             {@link AbstractComponentConnector#hasEventListener(String)}
     *             instead
     */
    @Deprecated
    public boolean hasEventListeners(Widget widget, String eventIdentifier) {
        ComponentConnector connector = getConnectorMap().getConnector(widget);
        if (connector == null) {
            /*
             * No connector will exist in cases where Vaadin widgets have been
             * re-used without implementing server<->client communication.
             */
            return false;
        }

        return hasEventListeners(connector, eventIdentifier);
    }

    LayoutManager getLayoutManager() {
        return null;
    }

    public void handleCommunicationError(String details, int statusCode) {
        boolean handled = false;
        if (communicationErrorDelegate != null) {
            handled = communicationErrorDelegate.onError(details, statusCode);

        }

        if (!handled) {
            showCommunicationError(details, statusCode);
        }

    }

    /**
     * Sets the delegate that is called whenever a communication error occurrs.
     *
     * @param delegate
     *            the delegate.
     */
    public void setCommunicationErrorDelegate(
            CommunicationErrorHandler delegate) {
        communicationErrorDelegate = delegate;
    }

    public void setApplicationRunning(boolean applicationRunning) {
        if (getApplicationState() == ApplicationState.TERMINATED) {
            if (applicationRunning) {
                getLogger().severe(
                        "Tried to restart a terminated application. This is not supported");
            } else {
                getLogger().warning(
                        "Tried to stop a terminated application. This should not be done");
            }
            return;
        } else if (getApplicationState() == ApplicationState.INITIALIZING) {
            if (applicationRunning) {
                applicationState = ApplicationState.RUNNING;
            } else {
                getLogger().warning(
                        "Tried to stop the application before it has started. This should not be done");
            }
        } else if (getApplicationState() == ApplicationState.RUNNING) {
            if (!applicationRunning) {
                applicationState = ApplicationState.TERMINATED;
            } else {
                getLogger().warning(
                        "Tried to start an already running application. This should not be done");
            }
        }
    }

    /**
     * Checks if the application is in the {@link ApplicationState#RUNNING}
     * state.
     *
     * @since 7.6
     * @return true if the application is in the running state, false otherwise
     */
    public boolean isApplicationRunning() {
        return applicationState == ApplicationState.RUNNING;
    }

    public <H extends EventHandler> HandlerRegistration addHandler(
            GwtEvent.Type<H> type, H handler) {
        return null;
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {

    }

    /**
     * Calls {@link ComponentConnector#flush()} on the active connector. Does
     * nothing if there is no active (focused) connector.
     */
    public void flushActiveConnector() {
        ComponentConnector activeConnector = getActiveConnector();
        if (activeConnector == null) {
            return;
        }
        activeConnector.flush();
    }

    /**
     * Gets the active connector for the focused element in the browser.
     *
     * @return the connector for the focused element or <code>null</code> if
     *         none found or no element is focused.
     */
    private ComponentConnector getActiveConnector() {
        Element focusedElement = WidgetUtil.getFocusedElement();
        if (focusedElement == null) {
            return null;
        }
        return Util.getConnectorForElement(this, RootPanel.get(),
                focusedElement);
    }

    private static Logger getLogger() {
        return Logger.getLogger(ApplicationConnection.class.getName());
    }

    /**
     * Returns the hearbeat instance.
     */
    public Heartbeat getHeartbeat() {
        return null;
    }

    /**
     * Returns the state of this application. An application state goes from
     * "initializing" to "running" to "stopped". There is no way for an
     * application to go back to a previous state, i.e. a stopped application
     * can never be re-started
     *
     * @since 7.6
     * @return the current state of this application
     */
    public ApplicationState getApplicationState() {
        return applicationState;
    }

    /**
     * Gets the server RPC queue for this application.
     *
     * @since 7.6
     * @return the server RPC queue
     */
    public ServerRpcQueue getServerRpcQueue() {
        return null;
    }

    /**
     * Gets the communication error handler for this application.
     *
     * @since 7.6
     * @return the server RPC queue
     */
    public ConnectionStateHandler getConnectionStateHandler() {
        return null;
    }

    /**
     * Gets the (server to client) message handler for this application.
     *
     * @since 7.6
     * @return the message handler
     */
    public MessageHandler getMessageHandler() {
        return null;
    }

    /**
     * Gets the server rpc manager for this application.
     *
     * @since 7.6
     * @return the server rpc manager
     */
    public RpcManager getRpcManager() {
        return null;
    }

    /**
     * Gets the (client to server) message sender for this application.
     *
     * @since 7.6
     * @return the message sender
     */
    public MessageSender getMessageSender() {
        return null;
    }

    /**
     * @since 7.6
     * @return the widget set
     */
    public WidgetSet getWidgetSet() {
        return widgetSet;
    }

    public int getLastSeenServerSyncId() {
        return 0;
    }

    /**
     * Gets the instance which handles loading of dependencies.
     *
     * @return the dependency loader for this connection
     */
    public DependencyLoader getDependencyLoader() {
        return null;
    }

}
