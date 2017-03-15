/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.rules.global;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.rules.asset.AssetRulesListPlace;
import org.openremote.manager.client.rules.tenant.TenantRulesListPlace;
import org.openremote.manager.shared.rules.GlobalRulesDefinition;
import org.openremote.manager.shared.rules.RulesResource;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class GlobalRulesEditorActivity
    extends AssetBrowsingActivity<GlobalRulesEditorPlace>
    implements GlobalRulesEditor.Presenter {

    final GlobalRulesEditor view;
    final GlobalRulesDefinitionMapper globalRulesDefinitionMapper;
    final RulesResource rulesResource;

    Long definitionId;
    GlobalRulesDefinition rulesDefinition;

    @Inject
    public GlobalRulesEditorActivity(Environment environment,
                                     AssetBrowser.Presenter assetBrowserPresenter,
                                     GlobalRulesEditor view,
                                     GlobalRulesDefinitionMapper globalRulesDefinitionMapper,
                                     RulesResource rulesResource) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.globalRulesDefinitionMapper = globalRulesDefinitionMapper;
        this.rulesResource = rulesResource;
    }

    @Override
    protected AppActivity<GlobalRulesEditorPlace> init(GlobalRulesEditorPlace place) {
        definitionId = place.getDefinitionId();
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            if (event.isTenantSelection()) {
                environment.getPlaceController().goTo(new TenantRulesListPlace(event.getSelectedNode().getRealm()));
            } else if (event.isAssetSelection()) {
                environment.getPlaceController().goTo(new AssetRulesListPlace(event.getSelectedNode().getId()));
            }
        }));

        if (definitionId != null) {
            environment.getRequestService().execute(
                globalRulesDefinitionMapper,
                params -> rulesResource.getGlobalDefinition(params, definitionId),
                200,
                rulesDefinition -> {
                    this.rulesDefinition = rulesDefinition;
                    writeToView();
                },
                ex -> handleRequestException(ex, environment)
            );
        } else {
            rulesDefinition = new GlobalRulesDefinition();
            writeToView();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    protected void writeToView() {
        view.setName(rulesDefinition.getName());
    }
}
