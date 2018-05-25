package org.openremote.test.assets

import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.console.ConsoleResourceImpl
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetResource
import org.openremote.model.asset.UserAsset
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

import static org.openremote.container.util.MapAccess.getString
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_ADMIN_PASSWORD
import static org.openremote.manager.setup.AbstractKeycloakSetup.SETUP_ADMIN_PASSWORD_DEFAULT
import static org.openremote.model.Constants.*

class AssetUserLinkingTest extends Specification implements ManagerContainerTrait {

    def "Link assets and users as superuser"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def timerService = container.getService(TimerService.class)
        def identityService = container.getService(ManagerIdentityService.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)

        and: "an authenticated admin user"
        def accessToken = authenticate(
                container,
                MASTER_REALM,
                KEYCLOAK_CLIENT_ID,
                MASTER_REALM_ADMIN_USER,
                getString(container.getConfig(), SETUP_ADMIN_PASSWORD, SETUP_ADMIN_PASSWORD_DEFAULT)
        ).token

        and: "the asset resource"
        def assetResource = getClientTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(AssetResource.class)

        /* ############################################## READ ####################################### */

        expect: "some users to be restricted"
        !identityService.getIdentityProvider().isRestrictedUser(keycloakDemoSetup.testuser1Id)
        !identityService.getIdentityProvider().isRestrictedUser(keycloakDemoSetup.testuser2Id)
        identityService.getIdentityProvider().isRestrictedUser(keycloakDemoSetup.testuser3Id)

        when: "all user assets are retrieved of a realm"
        def userAssets = assetResource.getUserAssetLinks(null, keycloakDemoSetup.customerATenant.id, null, null)

        then: "result should match"
        userAssets.length == 6
        userAssets.any {
            it.id.realmId == keycloakDemoSetup.customerATenant.id &&
                    it.id.userId == keycloakDemoSetup.testuser3Id &&
                    it.id.assetId == managerDemoSetup.apartment1Id &&
                    it.assetName == "Apartment 1" &&
                    it.parentAssetName == "Smart Home" &&
                    it.userFullName == "testuser3 (Testuserfirst Testuserlast)"
        }
        userAssets.any {
            it.id.realmId == keycloakDemoSetup.customerATenant.id &&
                    it.id.userId == keycloakDemoSetup.testuser3Id &&
                    it.id.assetId == managerDemoSetup.apartment1LivingroomId &&
                    it.assetName == "Living Room" &&
                    it.parentAssetName == "Apartment 1" &&
                    it.userFullName == "testuser3 (Testuserfirst Testuserlast)"
        }
        userAssets.any {
            it.id.realmId == keycloakDemoSetup.customerATenant.id &&
                    it.id.userId == keycloakDemoSetup.testuser3Id &&
                    it.id.assetId == managerDemoSetup.apartment1KitchenId &&
                    it.assetName == "Kitchen" &&
                    it.parentAssetName == "Apartment 1" &&
                    it.userFullName == "testuser3 (Testuserfirst Testuserlast)"
        }
        userAssets.any {
            it.id.realmId == keycloakDemoSetup.customerATenant.id &&
                    it.id.userId == keycloakDemoSetup.testuser3Id &&
                    it.id.assetId == managerDemoSetup.apartment1HallwayId &&
                    it.assetName == "Hallway" &&
                    it.parentAssetName == "Apartment 1" &&
                    it.userFullName == "testuser3 (Testuserfirst Testuserlast)"
        }
        userAssets.any {
            it.id.realmId == keycloakDemoSetup.customerATenant.id &&
                    it.id.userId == keycloakDemoSetup.testuser3Id &&
                    it.id.assetId == managerDemoSetup.apartment2Id &&
                    it.assetName == "Apartment 2" &&
                    it.parentAssetName == "Smart Home" &&
                    it.userFullName == "testuser3 (Testuserfirst Testuserlast)"
        }
        userAssets.any {
            it.id.realmId == keycloakDemoSetup.customerATenant.id &&
                    it.id.userId == keycloakDemoSetup.testuser3Id &&
                    it.id.assetId == managerDemoSetup.consoleId &&
                    it.assetName == "Demo Console" &&
                    it.parentAssetName == ConsoleResourceImpl.CONSOLE_PARENT_ASSET_NAME &&
                    it.userFullName == "testuser3 (Testuserfirst Testuserlast)"
        }

        when: "all user assets are retrieved of a realm and user"
        userAssets = assetResource.getUserAssetLinks(null, keycloakDemoSetup.customerATenant.id, keycloakDemoSetup.testuser3Id, null)

        then: "result should match"
        userAssets.length == 5

        when: "the realm and user don't match"
        assetResource.getUserAssetLinks(null, keycloakDemoSetup.customerBTenant.id, keycloakDemoSetup.testuser3Id, null)

        then: "an error response should be returned"
        WebApplicationException ex = thrown()
        ex.response.status == 400

        when: "the realm doesn't exist"
        assetResource.getUserAssetLinks(null, "doesnotexist", keycloakDemoSetup.testuser3Id, null)

        then: "an error response should be returned"
        ex = thrown()
        ex.response.status == 404

        when: "all user assets are retrieved of a realm and user"
        userAssets = assetResource.getUserAssetLinks(null, keycloakDemoSetup.customerATenant.id, null, managerDemoSetup.apartment1Id)

        then: "result should match"
        userAssets.length == 1
        userAssets.any {
            it.id.realmId == keycloakDemoSetup.customerATenant.id &&
                    it.id.userId == keycloakDemoSetup.testuser3Id &&
                    it.id.assetId == managerDemoSetup.apartment1Id &&
                    it.assetName == "Apartment 1" &&
                    it.parentAssetName == "Smart Home" &&
                    it.userFullName == "testuser3 (Testuserfirst Testuserlast)"
        }

        when: "all user assets are retrieved of a realm and user and asset"
        userAssets = assetResource.getUserAssetLinks(null, keycloakDemoSetup.customerATenant.id, keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1Id)

        then: "result should match"
        userAssets.length == 1
        userAssets.any {
            it.id.realmId == keycloakDemoSetup.customerATenant.id &&
                    it.id.userId == keycloakDemoSetup.testuser3Id &&
                    it.id.assetId == managerDemoSetup.apartment1Id &&
                    it.assetName == "Apartment 1" &&
                    it.parentAssetName == "Smart Home" &&
                    it.userFullName == "testuser3 (Testuserfirst Testuserlast)"
        }

        when: "all user assets are retrieved of a realm and user and asset"
        userAssets = assetResource.getUserAssetLinks(null, keycloakDemoSetup.customerATenant.id, keycloakDemoSetup.testuser2Id, managerDemoSetup.apartment1Id)

        then: "result should match"
        userAssets.length == 0

        /* ############################################## WRITE ####################################### */

        when: "an asset is linked to a user"
        UserAsset userAsset = new UserAsset(keycloakDemoSetup.customerATenant.id, keycloakDemoSetup.testuser2Id, managerDemoSetup.apartment2Id)
        assetResource.createUserAsset(null, userAsset)
        userAssets = assetResource.getUserAssetLinks(null, keycloakDemoSetup.customerATenant.id, keycloakDemoSetup.testuser2Id, null)

        then: "result should match"
        identityService.getIdentityProvider().isRestrictedUser(keycloakDemoSetup.testuser2Id)
        userAssets.length == 1
        userAssets.any {
            it.id.realmId == keycloakDemoSetup.customerATenant.id &&
                    it.id.userId == keycloakDemoSetup.testuser2Id &&
                    it.id.assetId == managerDemoSetup.apartment2Id &&
                    it.assetName == "Apartment 2" &&
                    it.parentAssetName == "Smart Home" &&
                    it.userFullName == "testuser2 (Testuserfirst Testuserlast)" &&
                    it.createdOn.time < timerService.currentTimeMillis
        }

        when: "an asset link is deleted"
        assetResource.deleteUserAsset(null, keycloakDemoSetup.customerATenant.id, keycloakDemoSetup.testuser2Id, managerDemoSetup.apartment2Id)
        userAssets = assetResource.getUserAssetLinks(null, keycloakDemoSetup.customerATenant.id, keycloakDemoSetup.testuser2Id, null)

        then: "result should match"
        !identityService.getIdentityProvider().isRestrictedUser(keycloakDemoSetup.testuser2Id)
        userAssets.length == 0

        cleanup: "the server should be stopped"
        stopContainer(container)
    }

}
