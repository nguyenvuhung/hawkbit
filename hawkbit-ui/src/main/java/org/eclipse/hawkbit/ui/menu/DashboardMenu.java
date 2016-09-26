/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/*
 * Licenses The source code is released under Apache 2.0. The application uses the Vaadin Charts 2
 * add-on, which is released under the Commercial Vaadin Addon License:
 * https://vaadin.com/license/cval-3
 */
package org.eclipse.hawkbit.ui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.HawkbitServerProperties;
import org.eclipse.hawkbit.im.authentication.PermissionService;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.menu.DashboardEvent.PostViewChangeEvent;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * A responsive menu component providing user information and the controls for
 * primary navigation between the views.
 */
@UIScope
@SpringComponent
public final class DashboardMenu extends CustomComponent {

    private static final String ID = "dashboard-menu";

    @Autowired
    private I18N i18n;

    @Autowired
    private transient UiProperties uiProperties;

    @Autowired
    private transient HawkbitServerProperties serverProperties;

    private static final long serialVersionUID = 5394474618559481462L;

    // this should be resolved when we introduce event bus on UI to just inform
    // the buttons directly via events
    private final List<ValoMenuItemButton> menuButtons = new ArrayList<>();

    @Autowired
    private transient PermissionService permissionService;

    @Autowired
    private final List<DashboardMenuItem> dashboardVaadinViews = new ArrayList<>();

    private String initialViewName;

    private boolean accessibleViewsEmpty;

    /**
     * initializing the view and creating the layout, cannot be done in the
     * constructor because the constructor will be called by spring and the
     * dashboard must be initialized when the dashboard UI is creating.
     */
    public void init() {
        initialViewName = "";
        addStyleName("valo-menu");
        setId(ID);
        setSizeUndefined();
        setHeight("100%");
        setCompositionRoot(buildContent());
    }

    private Component buildContent() {
        final VerticalLayout dashboardMenuLayout = new VerticalLayout();
        dashboardMenuLayout.setSizeFull();
        final VerticalLayout menuContent = getMenuLayout();
        menuContent.addComponent(buildTitle());
        menuContent.addComponent(buildUserMenu());
        menuContent.addComponent(buildToggleButton());

        final VerticalLayout menus = buildMenuItems();
        final VerticalLayout links = buildLinksAndVersion();
        menus.addComponent(links);
        menus.setComponentAlignment(links, Alignment.BOTTOM_CENTER);
        menus.setExpandRatio(links, 1.0F);
        menuContent.addComponent(menus);
        menuContent.setExpandRatio(menus, 1.0F);

        dashboardMenuLayout.addComponent(menuContent);
        return dashboardMenuLayout;
    }

    private VerticalLayout getMenuLayout() {
        final VerticalLayout menuContent = new VerticalLayout();
        menuContent.addStyleName(ValoTheme.MENU_PART);
        menuContent.addStyleName("sidebar");

        menuContent.addStyleName("no-vertical-drag-hints");
        menuContent.addStyleName("no-horizontal-drag-hints");
        menuContent.setWidth(null);
        menuContent.setHeight("100%");
        return menuContent;
    }

    private Component buildTitle() {
        final Label logo = new Label("<strong>" + i18n.get("menu.title") + "</strong>", ContentMode.HTML);
        logo.setSizeUndefined();
        final HorizontalLayout logoWrapper = new HorizontalLayout(logo);
        logoWrapper.setComponentAlignment(logo, Alignment.TOP_CENTER);
        logoWrapper.addStyleName("valo-menu-title");
        return logoWrapper;
    }

    private VerticalLayout buildLinksAndVersion() {
        final VerticalLayout links = new VerticalLayout();
        links.setSpacing(true);
        links.addStyleName("links");
        final String linkStyle = "v-link";

        if (!uiProperties.getLinks().getDocumentation().getRoot().isEmpty()) {
            final Link docuLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_DOCUMENTATION,
                    i18n.get("link.documentation.name"), uiProperties.getLinks().getDocumentation().getRoot(),
                    FontAwesome.QUESTION_CIRCLE, "_blank", linkStyle);
            docuLink.setDescription(i18n.get("link.documentation.name"));
            docuLink.setSizeFull();
            links.addComponent(docuLink);
            links.setComponentAlignment(docuLink, Alignment.BOTTOM_CENTER);
        }

        if (!uiProperties.getLinks().getUserManagement().isEmpty()) {
            final Link userManagementLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_USERMANAGEMENT,
                    i18n.get("link.usermanagement.name"), uiProperties.getLinks().getUserManagement(),
                    FontAwesome.USERS, "_blank", linkStyle);
            userManagementLink.setDescription(i18n.get("link.usermanagement.name"));
            links.addComponent(userManagementLink);
            userManagementLink.setSizeFull();
            links.setComponentAlignment(userManagementLink, Alignment.BOTTOM_CENTER);
        }

        if (!uiProperties.getLinks().getSupport().isEmpty()) {
            final Link supportLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_SUPPORT,
                    i18n.get("link.support.name"), uiProperties.getLinks().getSupport(), FontAwesome.ENVELOPE_O, "",
                    linkStyle);
            supportLink.setDescription(i18n.get("link.support.name"));
            supportLink.setSizeFull();
            links.addComponent(supportLink);
            links.setComponentAlignment(supportLink, Alignment.BOTTOM_CENTER);

        }

        final Component buildVersionInfo = buildVersionInfo();
        links.addComponent(buildVersionInfo);
        links.setComponentAlignment(buildVersionInfo, Alignment.BOTTOM_CENTER);
        links.setSizeFull();
        links.setHeightUndefined();
        return links;
    }

    private Component buildUserMenu() {
        final MenuBar settings = new MenuBar();
        settings.addStyleName("user-menu");
        settings.setHtmlContentAllowed(true);
        final MenuItem settingsItem = settings.addItem("", new ThemeResource("images/profile-pic-57px.jpg"), null);

        final String formattedTenant = UserDetailsFormatter.formatCurrentTenant();
        final String formattedUsername = UserDetailsFormatter.formatCurrentUsername();
        String tenantAndUsernameHtml = "";
        if (!StringUtils.isEmpty(formattedTenant)) {
            tenantAndUsernameHtml += formattedTenant + "<br>";
        }
        tenantAndUsernameHtml += formattedUsername;
        settingsItem.setText(tenantAndUsernameHtml);
        settingsItem.setDescription(formattedUsername);
        settingsItem.setStyleName("user-menuitem");

        settingsItem.addItem("Sign Out", selectedItem -> Page.getCurrent().setLocation("/UI/logout"));
        return settings;
    }

    private Component buildToggleButton() {
        final Button valoMenuToggleButton = new Button("Menu", new MenuToggleClickListenerMyClickListener());
        valoMenuToggleButton.setIcon(FontAwesome.LIST);
        valoMenuToggleButton.addStyleName("valo-menu-toggle");
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_SMALL);
        return valoMenuToggleButton;
    }

    private VerticalLayout buildMenuItems() {
        final VerticalLayout menuItemsLayout = new VerticalLayout();
        menuItemsLayout.addStyleName("valo-menuitems");
        menuItemsLayout.setHeight(100.0F, Unit.PERCENTAGE);

        final List<DashboardMenuItem> accessibleViews = getAccessibleViews();
        if (accessibleViews.isEmpty()) {
            accessibleViewsEmpty = true;
            return menuItemsLayout;
        }
        initialViewName = accessibleViews.get(0).getViewName();
        for (final DashboardMenuItem view : accessibleViews) {
            final ValoMenuItemButton menuItemComponent = new ValoMenuItemButton(view);
            menuButtons.add(menuItemComponent);
            menuItemsLayout.addComponent(menuItemComponent);
        }
        return menuItemsLayout;
    }

    /**
     * Returns all views which are currently accessible by the current logged in
     * user.
     *
     * @return a list of all views which are currently visible and accessible
     *         for the current logged in user
     */
    private List<DashboardMenuItem> getAccessibleViews() {
        return this.dashboardVaadinViews.stream()
                .filter(view -> permissionService.hasAtLeastOnePermission(view.getPermissions()))
                .collect(Collectors.toList());
    }

    private Component buildVersionInfo() {
        final Label label = new Label();
        label.setSizeFull();
        label.setStyleName("version-layout");
        label.setValue(serverProperties.getBuild().getVersion());
        return label;
    }

    /**
     * Returns the view name for the start page after login.
     *
     * @return the initialViewName of the start page
     */
    public String getInitialViewName() {
        return initialViewName;
    }

    /**
     * Is a View available.
     *
     * @return the accessibleViewsEmpty <true> no rights for any view <false> a
     *         view is available
     */
    public boolean isAccessibleViewsEmpty() {
        return accessibleViewsEmpty;
    }

    /**
     * notifies the dashboard that the view has been changed and the button
     * needs to be re-styled.
     *
     * @param event
     *            the post view change event
     */
    public void postViewChange(final PostViewChangeEvent event) {
        menuButtons.forEach(button -> button.postViewChange(event));
    }

    /**
     * Returns the dashboard view type by a given view name.
     *
     * @param viewName
     *            the name of the view to retrieve
     * @return the dashboard view for a given viewname or {@code null} if view
     *         with given viewname does not exists
     */
    public DashboardMenuItem getByViewName(final String viewName) {
        final Optional<DashboardMenuItem> findFirst = dashboardVaadinViews.stream()
                .filter(view -> view.getViewName().equals(viewName)).findFirst();

        if (!findFirst.isPresent()) {
            return null;
        }

        return findFirst.get();
    }

    /**
     * Is the given view accessible.
     *
     * @param viewName
     *            the view name
     * @return <true> = denied, <false> = accessible
     */
    public boolean isAccessDenied(final String viewName) {
        final List<DashboardMenuItem> accessibleViews = getAccessibleViews();
        boolean accessDeined = Boolean.TRUE.booleanValue();
        for (final DashboardMenuItem dashboardViewType : accessibleViews) {
            if (dashboardViewType.getViewName().equals(viewName)) {
                accessDeined = Boolean.FALSE.booleanValue();
            }
        }
        return accessDeined;
    }

    private class MenuToggleClickListenerMyClickListener implements ClickListener {

        private static final long serialVersionUID = 1L;
        private static final String STYLE_VISIBLE = "valo-menu-visible";

        @Override
        public void buttonClick(final ClickEvent event) {
            if (getCompositionRoot().getStyleName().contains(STYLE_VISIBLE)) {
                getCompositionRoot().removeStyleName(STYLE_VISIBLE);
            } else {
                getCompositionRoot().addStyleName(STYLE_VISIBLE);
            }
        }
    }

    /**
     * An menu item button wrapper for the dashboard menu item.
     */
    public static final class ValoMenuItemButton extends Button {

        private static final long serialVersionUID = 1L;

        private static final String STYLE_SELECTED = "selected";

        private final DashboardMenuItem view;

        /**
         * creates a new button in case of pressed switches to the given
         * {@code view}.
         *
         * @param view
         *            the view to switch to in case the button is pressed
         */
        public ValoMenuItemButton(final DashboardMenuItem view) {
            this.view = view;
            setPrimaryStyleName("valo-menu-item");
            setIcon(view.getDashboardIcon());
            setCaption(view.getDashboardCaption());
            setDescription(view.getDashboardCaptionLong());
            /* Avoid double click */
            setDisableOnClick(true);
            addClickListener(event -> event.getComponent().getUI().getNavigator().navigateTo(view.getViewName()));
        }

        /**
         * notifies the button to change his style.
         *
         * @param event
         *            the post view change event
         */
        public void postViewChange(final PostViewChangeEvent event) {
            removeStyleName(STYLE_SELECTED);
            if (event.getView().equals(view)) {
                addStyleName(STYLE_SELECTED);
                /* disable current selected view */
                setEnabled(false);
            } else {
                /* Enable other views */
                setEnabled(true);
            }
        }
    }
}
