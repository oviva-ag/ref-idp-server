/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.idp.server;

import static de.gematik.idp.TestConstants.REDIRECT_URI_E_REZEPT_APP;
import static de.gematik.idp.brainPoolExtension.BrainpoolAlgorithmSuiteIdentifiers.BRAINPOOL256_USING_SHA256;
import static de.gematik.idp.client.AuthenticatorClient.getAllHeaderElementsAsMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.gematik.idp.TestConstants;
import de.gematik.idp.authentication.AuthenticationChallenge;
import de.gematik.idp.authentication.IdpJwtProcessor;
import de.gematik.idp.authentication.UriUtils;
import de.gematik.idp.client.IdpClient;
import de.gematik.idp.client.IdpClientRuntimeException;
import de.gematik.idp.client.IdpTokenResult;
import de.gematik.idp.client.data.AuthenticationResponse;
import de.gematik.idp.client.data.AuthorizationResponse;
import de.gematik.idp.crypto.model.PkiIdentity;
import de.gematik.idp.field.ClaimName;
import de.gematik.idp.field.IdpScope;
import de.gematik.idp.server.configuration.IdpConfiguration;
import de.gematik.idp.server.controllers.IdpKey;
import de.gematik.idp.tests.Afo;
import de.gematik.idp.tests.PkiKeyResolver;
import de.gematik.idp.tests.PkiKeyResolver.Filename;
import de.gematik.idp.tests.Remark;
import de.gematik.idp.tests.Rfc;
import de.gematik.idp.token.IdpJoseObject;
import de.gematik.idp.token.IdpJwe;
import de.gematik.idp.token.JsonWebToken;
import java.security.Key;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ExtendWith(PkiKeyResolver.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TokenRetrievalTest {

    private static final String SHA256_AS_BASE64_REGEX = "^[_\\-a-zA-Z0-9]{42,44}[=]{0,2}$";

    @Autowired
    private IdpConfiguration idpConfiguration;
    @Autowired
    private IdpKey idpSig;
    @Autowired
    private Key symmetricEncryptionKey;
    private IdpClient idpClient;
    private PkiIdentity egkUserIdentity;
    @LocalServerPort
    private int localServerPort;

    @BeforeEach
    public void startup(
        @Filename("109500969_X114428530_c.ch.aut-ecc") final PkiIdentity egkIdentity) {
        idpClient = IdpClient.builder()
            .clientId(TestConstants.CLIENT_ID_E_REZEPT_APP)
            .discoveryDocumentUrl("http://localhost:" + localServerPort + "/discoveryDocument")
            .redirectUrl(REDIRECT_URI_E_REZEPT_APP)
            .build();

        idpClient.initialize();

        egkUserIdentity = egkIdentity;
    }

    @Rfc({"OpenID Connect Core 1.0 incorporating errata set 1 - 3.1.3.3.  Successful Token Response",
        "RFC6750 October 2012 - 4.  Example Access Token Response"})
    @Afo("A_20463")
    @Remark("ACCESS_TOKEN ist nur 300 s lang gültig, also auch die Response.")
    @Test
    public void verifyExpiresInTokenResponse() throws UnirestException {
        final IdpTokenResult tokenResponse = idpClient.login(egkUserIdentity);

        assertThat(tokenResponse.getExpiresIn())
            .as("ExpiresIn")
            .isEqualTo(300);
    }

    @Rfc({"OpenID Connect Core 1.0 incorporating errata set 1 - 3.1.3.3.  Successful Token Response",
        "RFC6750 October 2012 - 4.  Example Access Token Response"})
    @Test
    public void verifyTokenType() throws UnirestException {
        final IdpTokenResult tokenResponse = idpClient.login(egkUserIdentity);

        assertThat(tokenResponse.getTokenType())
            .as("TokenType")
            .isEqualTo("Bearer");
    }

    @Test
    public void getAccessTokenWithRsa(
        @Filename("833621999741600_c.hci.aut-apo-rsa") final PkiIdentity rsaEgkIdentity) throws UnirestException {
        final IdpTokenResult tokenResponse = idpClient.login(rsaEgkIdentity);

        assertThat(tokenResponse.getTokenType())
            .as("TokenType")
            .isEqualTo("Bearer");
    }

    @Test
    public void authenticationForwardShouldContainSsoToken() throws UnirestException {
        idpClient.setAfterAuthenticationCallback(response ->
            assertThat(response.getHeaders().getFirst("Location"))
                .contains("ssotoken="));

        idpClient.login(egkUserIdentity);
    }

    @Rfc("OpenID Connect Core 1.0 incorporating errata set 1 - 3.1.3.3.  Successful Token Response")
    @Test
    public void authenticationHttpHeaderShouldContainCacheControl() throws UnirestException {
        idpClient.setAfterAuthenticationCallback(response ->
            assertThat(response.getHeaders().getFirst("Cache-Control"))
                .contains("no-store"));

        idpClient.login(egkUserIdentity);
    }

    @Rfc("OpenID Connect Core 1.0 incorporating errata set 1 - 3.1.3.3.  Successful Token Response")
    @Test
    public void authenticationHttpHeaderShouldContainPragma() throws UnirestException {
        idpClient.setAfterAuthenticationCallback(response ->
            assertThat(response.getHeaders().getFirst("Pragma"))
                .contains("no-cache"));

        idpClient.login(egkUserIdentity);
    }

    @Rfc("OpenID Connect Core 1.0 incorporating errata set 1 - 3.1.3.3.  Successful Token Response")
    @Test
    public void authorizationHttpHeaderShouldContainCacheControl() throws UnirestException {
        idpClient.setAfterAuthorizationCallback(response ->
            assertThat(response.getHeaders().getFirst("Cache-Control"))
                .contains("no-store"));

        idpClient.login(egkUserIdentity);
    }

    @Rfc("OpenID Connect Core 1.0 incorporating errata set 1 - 3.1.3.3.  Successful Token Response")
    @Test
    public void authorizationHttpHeaderShouldContainPragma() throws UnirestException {
        idpClient.setAfterAuthorizationCallback(response ->
            assertThat(response.getHeaders().getFirst("Pragma"))
                .contains("no-cache"));

        idpClient.login(egkUserIdentity);
    }

    @Rfc("OpenID Connect Core 1.0 incorporating errata set 1 - 3.1.3.3.  Successful Token Response")
    @Test
    public void tokenResponseHttpHeaderShouldContainCacheControl() throws UnirestException {
        idpClient.setAfterTokenCallback(response ->
            assertThat(response.getHeaders().getFirst("Cache-Control"))
                .contains("no-store"));

        idpClient.login(egkUserIdentity);
    }

    @Rfc("OpenID Connect Core 1.0 incorporating errata set 1 - 3.1.3.3.  Successful Token Response")
    @Test
    public void tokenResponseHttpHeaderShouldContainPragma() throws UnirestException {
        idpClient.setAfterTokenCallback(response ->
            assertThat(response.getHeaders().getFirst("Pragma"))
                .contains("no-cache"));

        idpClient.login(egkUserIdentity);
    }

    @Test
    public void ssoTokenAuthorizationShouldReturnCode() throws UnirestException {
        final AtomicReference<String> code = new AtomicReference();
        idpClient.setAfterAuthenticationCallback(response ->
            Optional.of(UriUtils.extractParameterValue(response.getHeaders().getFirst("Location"), "code"))
                .ifPresent(auth_code -> code.set(auth_code)));

        idpClient.loginWithSsoToken(idpClient.login(egkUserIdentity).getSsoToken());

        assertThat(code).isNotNull();
    }

    @Test
    public void getNewAuthenticationCodeViaSsoToken_ResponseURLShouldNotContainSsoToken() throws UnirestException {
        final AtomicReference<String> ssoToken = new AtomicReference();
        idpClient.setAfterAuthenticationCallback(response ->
            Optional.ofNullable(
                UriUtils.extractParameterValue(response.getHeaders().getFirst("Location"), "ssotoken"))
                .ifPresent(token -> ssoToken.set(token)));

        assertThatThrownBy(() -> idpClient.loginWithSsoToken(idpClient.login(egkUserIdentity).getSsoToken()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("ssotoken");
    }

    @Test
    public void getNewAccessTokenViaSsoToken_NewAccessTokenShouldHaveLongerValidity() throws UnirestException {
        final IdpTokenResult oldLoginResult = idpClient.login(egkUserIdentity);
        final JsonWebToken newLoginResult = idpClient.loginWithSsoToken(oldLoginResult.getSsoToken()).getAccessToken();

        assertThat(oldLoginResult.getAccessToken().getExpiresAtBody())
            .isBeforeOrEqualTo(newLoginResult.getExpiresAtBody());
    }

    @Test
    public void loginWithoutAndThenWithSsoToken_codeChallengeShouldDiffer() throws UnirestException {
        final AtomicReference<JsonWebToken> oldCodeChallenge = new AtomicReference();
        final AtomicReference<JsonWebToken> newCodeChallenge = new AtomicReference();

        idpClient.setAfterAuthenticationCallback(
            response -> oldCodeChallenge.set(extractAuthenticationTokenFromResponse(response, "code")));

        final IdpTokenResult oldLoginResult = idpClient.login(egkUserIdentity);

        idpClient.setAfterAuthenticationCallback(
            response -> newCodeChallenge.set(extractAuthenticationTokenFromResponse(response, "code")));

        idpClient.loginWithSsoToken(oldLoginResult.getSsoToken()).getAccessToken();

        assertThat(oldCodeChallenge.get().getBodyClaim(ClaimName.CODE_CHALLENGE).get())
            .isNotEqualTo(newCodeChallenge.get().getBodyClaim(ClaimName.CODE_CHALLENGE).get());
    }

    @Test
    public void ssoShouldBeEncrypted() throws UnirestException {
        final IdpTokenResult tokenResult = idpClient.login(egkUserIdentity);

        assertThatThrownBy(() -> tokenResult.getSsoToken().getBodyClaims())
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void ssoTokenCnfClaimShouldBeJsonObject() throws UnirestException {
        final IdpTokenResult tokenResult = idpClient.login(egkUserIdentity);

        assertThat(tokenResult.getSsoToken().decryptNestedJwt(symmetricEncryptionKey)
            .getBodyClaim(ClaimName.CONFIRMATION).get())
            .isInstanceOf(Map.class);
    }

    @Test
    public void ssoTokenShouldNotContainNjwtClaim() throws UnirestException {
        final IdpTokenResult tokenResult = idpClient.login(egkUserIdentity);
        assertThat(tokenResult.getSsoToken().decryptNestedJwt(symmetricEncryptionKey)
            .getBodyClaims())
            .doesNotContainKey(ClaimName.NESTED_JWT.getJoseName());
    }

    @Test
    public void verifyTokenAlgorithm() throws UnirestException {
        final IdpTokenResult tokenResponse = idpClient.login(egkUserIdentity);

        assertThat(tokenResponse.getAccessToken().getHeaderClaim(ClaimName.ALGORITHM))
            .get()
            .isEqualTo(BRAINPOOL256_USING_SHA256);
    }

    @Test
    public void verifyTokenContainsAcr() throws UnirestException {
        final IdpTokenResult tokenResponse = idpClient.login(egkUserIdentity);

        assertThat(tokenResponse.getAccessToken().getBodyClaim(ClaimName.AUTHENTICATION_CLASS_REFERENCE))
            .get()
            .isEqualTo("gematik-ehealth-loa-high");
    }

    @Test
    public void verifyTokenContainsGematikClaims() throws UnirestException {
        final IdpTokenResult tokenResponse = idpClient.login(egkUserIdentity);

        assertThat(tokenResponse.getAccessToken().getBodyClaim(ClaimName.PROFESSION_OID))
            .isPresent(); // This is the most robust claim
    }

    @Test
    public void verifyTokenContainsCorrectSubClaim() throws UnirestException {
        final IdpTokenResult tokenResponse = idpClient.login(egkUserIdentity);

        assertThat(tokenResponse.getAccessToken().getStringBodyClaim(ClaimName.SUBJECT))
            .get().asString()
            .matches(SHA256_AS_BASE64_REGEX);
    }

    @Test
    public void verifySubClaimMatchesInIdTokenAndAccessToken() throws UnirestException {
        final IdpTokenResult tokenResponse = idpClient.login(egkUserIdentity);

        assertThat(tokenResponse.getAccessToken().getStringBodyClaim(ClaimName.SUBJECT))
            .get().asString()
            .isEqualTo(tokenResponse.getIdToken().getStringBodyClaim(ClaimName.SUBJECT).get());
    }

    @Test
    public void verifyNonceClaimCorrectInIdToken() throws UnirestException {
        final AtomicReference<String> nonceValue = new AtomicReference<>();
        idpClient.setBeforeAuthorizationCallback(request ->
            nonceValue.set(UriUtils.extractParameterValue(request.getUrl(), "nonce")));

        final IdpTokenResult tokenResponse = idpClient.login(egkUserIdentity);

        assertThat(tokenResponse.getIdToken().getStringBodyClaim(ClaimName.NONCE).get())
            .isEqualTo(nonceValue.get())
            .isNotEmpty();
    }

    @Test
    public void assertThatTokenIsValid() throws UnirestException {
        final IdpTokenResult tokenResponse = idpClient.login(egkUserIdentity);

        idpClient.verifyAuthTokenToken(tokenResponse);
    }

    @Test
    public void testSmcbLogin(@Filename("c.hci.aut-apo-ecc") final PkiIdentity smcbEccIdentity)
        throws UnirestException {
        final PkiIdentity smcbIdentity = PkiIdentity.builder()
            .certificate(smcbEccIdentity.getCertificate())
            .privateKey(smcbEccIdentity.getPrivateKey())
            .build();

        final IdpTokenResult tokenResponse = idpClient.login(smcbIdentity);

        idpClient.verifyAuthTokenToken(tokenResponse);
    }

    @Test
    public void testLoginHba(@Filename("80276883110000129084-C_HP_AUT_E256") final PkiIdentity failIdentity)
        throws UnirestException {
        final PkiIdentity smcbIdentity = PkiIdentity.builder()
            .certificate(failIdentity.getCertificate())
            .privateKey(failIdentity.getPrivateKey())
            .build();

        final IdpTokenResult tokenResponse = idpClient.login(smcbIdentity);

        idpClient.verifyAuthTokenToken(tokenResponse);
    }

    @Afo("A_20376")
    @Test
    public void stateParameterNotGivenInInitialRequest_ServerShouldGiveError()
        throws UnirestException {
        idpClient.setBeforeAuthorizationMapper(request -> Unirest
            .get(request.getUrl().replaceFirst("&state=[\\w-_.~]*", ""))
            .headers(getAllHeaderElementsAsMap(request)));

        assertThatThrownBy(() -> idpClient.login(egkUserIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("2002", "state wurde nicht übermittelt");
    }

    @Afo("A_20377")
    @Test
    public void stateParameterShouldBeEqualInFirstAndLastRequest() throws UnirestException {
        idpClient.setBeforeAuthorizationMapper(request -> Unirest
            .get(request.getUrl()
                .replaceFirst("&state=[\\w-_.~]*", "&state=foobar"))
            .headers(getAllHeaderElementsAsMap(request)));

        idpClient.setAfterAuthenticationCallback(request -> assertThat(
            Optional.ofNullable(request.getHeaders().getFirst("Location"))
                .map(location -> UriUtils.extractParameterValue(location, "state"))
                .orElseThrow())
            .isEqualTo("foobar"));

        idpClient.login(egkUserIdentity);
    }

    @Afo("A_20376")
    @Test
    public void stateParameterGiven_shouldBePresentInRedirect() throws UnirestException {
        idpClient.setAfterAuthenticationCallback(
            request -> assertThat(Optional.ofNullable(request.getHeaders().getFirst("Location"))
                .map(location -> UriUtils.extractParameterValue(location, "state"))
                .orElseThrow())
                .isNotBlank());

        idpClient.login(egkUserIdentity);
    }

    @Rfc("RFC6749, 4.1.3")
    @Test
    public void missmatchedRedirectUri_shouldGiveErrorOnTokenRetrieval() throws UnirestException {
        idpClient.setBeforeTokenCallback(body -> body.field("redirect_uri", "wrongValue"));

        assertThatThrownBy(() -> idpClient.login(egkUserIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("1020", "redirect_uri ist ungültig");
    }

    @Test
    public void scopeWithoutErezept_shouldGiveServerError() throws UnirestException {
        idpClient.setScopes(Set.of(IdpScope.OPENID));

        assertThatThrownBy(() -> idpClient.login(egkUserIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("1022", "scope ist ungültig");
    }

    @Test
    public void resignedChallengeTokenWithDifferentIdentity_ShouldGiveServerError(
        @Filename("80276883110000129084-C_HP_AUT_E256.p12") final PkiIdentity notTheServerIdentity)
        throws UnirestException {
        final IdpJwtProcessor differentSigner = new IdpJwtProcessor(notTheServerIdentity);
        idpClient.setAuthorizationResponseMapper(response -> {
            final JsonWebToken originalChallenge = response.getAuthenticationChallenge().getChallenge();
            final JsonWebToken resignedChallenge = differentSigner.buildJwt(originalChallenge.toJwtDescription());
            return AuthorizationResponse.builder()
                .authenticationChallenge(AuthenticationChallenge.builder()
                    .userConsent(response.getAuthenticationChallenge().getUserConsent())
                    .challenge(resignedChallenge)
                    .build())
                .build();
        });

        assertThatThrownBy(() -> idpClient.login(egkUserIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("2013", "Der Request besitzt keine gültige Signatur");
    }

    @Test
    public void resignedAuthenticationTokenWithDifferentIdentity_ShouldGiveServerError(
        @Filename("80276883110000129084-C_HP_AUT_E256") final PkiIdentity notTheServerIdentity)
        throws UnirestException {
        final IdpJwtProcessor differentSigner = new IdpJwtProcessor(notTheServerIdentity);
        idpClient.setAuthenticationResponseMapper(response -> {
            final JsonWebToken originalChallenge = new IdpJwe(response.getCode())
                .decryptNestedJwt(symmetricEncryptionKey);
            final JsonWebToken resignedChallenge = differentSigner.buildJwt(originalChallenge.toJwtDescription());
            return AuthenticationResponse.builder()
                .ssoToken(response.getSsoToken())
                .location(response.getLocation())
                .code(resignedChallenge.getRawString())
                .build();
        });

        assertThatThrownBy(() -> idpClient.login(egkUserIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("3010", "Authorization Code Signatur ungültig");
    }

    @Test
    public void scopeWithoutOpenid_shouldGiveException() throws UnirestException {
        idpClient.setScopes(Set.of(IdpScope.EREZEPT));

        assertThatThrownBy(() -> idpClient.login(egkUserIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("1022", "scope ist ungültig");
    }

    @Test
    public void requestChallenge_testServerChallengeClaims() throws UnirestException {
        idpClient.setAfterAuthorizationCallback(response ->
            assertThat(response.getBody().getChallenge().getBodyClaims())
                .containsEntry(ClaimName.TOKEN_TYPE.getJoseName(), "challenge"));

        idpClient.login(egkUserIdentity);
    }

    @Test
    public void getAuthorizationToken_testBodyClaims() throws UnirestException {
        idpClient.setAuthenticationResponseMapper(response -> {
            assertThat(new IdpJwe(response.getCode()).decryptNestedJwt(symmetricEncryptionKey).getBodyClaims())
                .containsEntry(ClaimName.CLIENT_ID.getJoseName(), TestConstants.CLIENT_ID_E_REZEPT_APP)
                .containsEntry(ClaimName.TOKEN_TYPE.getJoseName(), "code")
                .containsKeys(ClaimName.SERVER_NONCE.getJoseName(),
                    ClaimName.NONCE.getJoseName())
                .doesNotContainKeys(ClaimName.AUTHENTICATION_CLASS_REFERENCE.getJoseName(),
                    ClaimName.SUBJECT.getJoseName(),
                    ClaimName.AUDIENCE.getJoseName());
            return response;
        });

        idpClient.login(egkUserIdentity);
    }

    @Test
    public void requestChallenge_shouldContainOriginalNonce() throws UnirestException {
        final AtomicReference<String> nonceValue = new AtomicReference();
        idpClient.setBeforeAuthorizationCallback(
            getRequest -> nonceValue.set(UriUtils.extractParameterValue(getRequest.getUrl(), "nonce")));
        idpClient.setAfterAuthorizationCallback(response ->
            assertThat(response.getBody().getChallenge().getBodyClaim(ClaimName.NONCE))
                .get().asString()
                .isEqualTo(nonceValue.get()));

        idpClient.login(egkUserIdentity);
    }

    @Test
    public void scopeOpenIdAndPairing_shouldGiveAccessToken() throws UnirestException {
        idpClient.setScopes(Set.of(IdpScope.OPENID, IdpScope.PAIRING));
        final IdpTokenResult loginResult = idpClient.login(egkUserIdentity);

        assertThat(loginResult.getAccessToken().getScopesBodyClaim())
            .containsExactlyInAnyOrder(IdpScope.OPENID, IdpScope.PAIRING);
    }

    @Test
    public void scopeOpenIdErezeptAndPairing_shouldGiveAccessToken() throws UnirestException {
        idpClient.setScopes(Set.of(IdpScope.OPENID, IdpScope.PAIRING, IdpScope.EREZEPT));
        final IdpTokenResult loginResult = idpClient.login(egkUserIdentity);

        assertThat(loginResult.getAccessToken().getScopesBodyClaim())
            .containsExactlyInAnyOrder(IdpScope.OPENID, IdpScope.PAIRING, IdpScope.EREZEPT);
    }

    @Test
    public void authentication_expiredChallenge_shouldGiveFoundAndCorrectState() throws UnirestException {
        final AtomicReference<String> stateReference = new AtomicReference<>();

        idpClient.setAuthorizationResponseMapper(response -> {
            stateReference
                .set(response.getAuthenticationChallenge().getChallenge().getStringBodyClaim(ClaimName.STATE).get());
            return AuthorizationResponse.builder()
                .authenticationChallenge(AuthenticationChallenge.builder()
                    .userConsent(response.getAuthenticationChallenge().getUserConsent())
                    .challenge(response.getAuthenticationChallenge().getChallenge().toJwtDescription()
                        .expiresAt(ZonedDateTime.now().minusMinutes(1))
                        .setIdentity(idpSig.getIdentity())
                        .buildJwt())
                    .build())
                .build();
        });

        assertThatThrownBy(() -> idpClient.login(egkUserIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("2032", "Challenge ist abgelaufen");
    }

    @Test
    public void ssoFlow_expiredChallenge_shouldGiveFoundAndCorrectState() throws UnirestException {
        final IdpJwe ssoToken = idpClient.login(egkUserIdentity).getSsoToken();

        idpClient.setAuthorizationResponseMapper(response -> AuthorizationResponse.builder()
            .authenticationChallenge(AuthenticationChallenge.builder()
                .userConsent(response.getAuthenticationChallenge().getUserConsent())
                .challenge(response.getAuthenticationChallenge().getChallenge().toJwtDescription()
                    .expiresAt(ZonedDateTime.now().minusMinutes(1))
                    .setIdentity(idpSig.getIdentity())
                    .buildJwt())
                .build())
            .build());

        assertThatThrownBy(() -> idpClient.loginWithSsoToken(ssoToken))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("2032", "Challenge ist abgelaufen");
    }

    @Test
    public void authenticationWithSso_missingParameter_shouldGiveError() throws UnirestException {
        final IdpJwe ssoToken = idpClient.login(egkUserIdentity).getSsoToken();
        idpClient.setBeforeAuthenticationMapper(request -> Unirest.post(request.getUrl())
            .multiPartContent().field("ssotoken", request.multiParts().stream()
                .filter(part -> part.getName().equals("ssotoken"))
                .findAny().get().getValue().toString())
            .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        );

        assertThatThrownBy(() -> idpClient.loginWithSsoToken(ssoToken))
            .isInstanceOf(IdpClientRuntimeException.class);
    }

    @Test
    public void illegalCertificateType_shouldGiveServerError(
        @Filename("smcb-idp-expired") final PkiIdentity illegalIdentity) throws UnirestException {
        assertThatThrownBy(() -> idpClient.login(illegalIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("2020", "Das AUT Zertifikat ist ungültig");
    }

    @Test
    public void expiredAuthenticationToken_shouldGiveValidationError() throws UnirestException {
        idpClient.setAuthenticationResponseMapper(authResponse -> {
            final IdpJoseObject expiredAuthToken = new IdpJwe(authResponse.getCode())
                .decryptNestedJwt(symmetricEncryptionKey)
                .toJwtDescription()
                .expiresAt(ZonedDateTime.now().minusSeconds(1))
                .setSignerKey(idpSig.getIdentity().getPrivateKey())
                .buildJwt()
                .encrypt(symmetricEncryptionKey);
            return AuthenticationResponse.builder()
                .code(expiredAuthToken.getRawString())
                .location(authResponse.getLocation())
                .ssoToken(authResponse.getSsoToken())
                .build();
        });

        idpClient.setAfterTokenCallback(response -> assertThat(response.getStatus()).isEqualTo(400));

        assertThatThrownBy(() -> idpClient.login(egkUserIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("3011", "Authorization Code ist abgelaufen");
    }

    @Test
    public void expiredSsoToken_shouldGiveValidationError() throws UnirestException {
        final IdpTokenResult tokenResult = idpClient.login(egkUserIdentity);

        final IdpJwe expiredSsoToken = tokenResult.getSsoToken()
            .decryptNestedJwt(symmetricEncryptionKey)
            .toJwtDescription()
            .expiresAt(ZonedDateTime.now().minusMinutes(10))
            .setSignerKey(idpSig.getIdentity().getPrivateKey())
            .buildJwt()
            .encrypt(symmetricEncryptionKey);

        assertThatThrownBy(() -> idpClient.loginWithSsoToken(expiredSsoToken))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("2040", "SSO_TOKEN nicht valide, bitte um neuerliche Authentisierung");
    }

    @Test
    public void expiredServerChallenge_shouldGiveValidationError() throws UnirestException {
        idpClient.setAuthorizationResponseMapper(authResponse -> {
            final JsonWebToken expiredChallenge = authResponse.getAuthenticationChallenge().getChallenge()
                .toJwtDescription()
                .expiresAt(ZonedDateTime.now().minusSeconds(1))
                .setSignerKey(idpSig.getIdentity().getPrivateKey())
                .buildJwt();
            return AuthorizationResponse.builder()
                .authenticationChallenge(AuthenticationChallenge.builder()
                    .challenge(expiredChallenge)
                    .userConsent(authResponse.getAuthenticationChallenge().getUserConsent())
                    .build())
                .build();
        });

        assertThatThrownBy(() -> idpClient.login(egkUserIdentity))
            .isInstanceOf(IdpClientRuntimeException.class)
            .hasMessageContaining("2032", "Challenge ist abgelaufen");
    }

    @Test
    public void verifyUserConsent() throws UnirestException {
        idpClient.setAfterAuthorizationCallback(
            response -> {
                assertThat(response.getBody().getUserConsent().getRequestedScopes())
                    .containsOnlyKeys("e-rezept", "openid");
                assertThat(response.getBody().getUserConsent().getRequestedClaims())
                    .containsOnlyKeys("organizationName", "professionOID", "idNummer", "given_name", "family_name");
            });

        idpClient.login(egkUserIdentity);
    }

    private JsonWebToken extractAuthenticationTokenFromResponse(final kong.unirest.HttpResponse<String> response,
        final String parameterName) {
        return Optional.ofNullable(response.getHeaders().getFirst("Location"))
            .map(uri -> UriUtils.extractParameterValue(uri, parameterName))
            .map(code -> new IdpJwe(code))
            .map(jwe -> jwe.decryptNestedJwt(symmetricEncryptionKey))
            .get();
    }

    @Test
    public void locationFrom_AuthenticationResponseHeader_should_startsWith_RedirectUri() throws UnirestException {
        idpClient.setAfterAuthenticationCallback(response -> {
            assertThat(response.getStatus())
                .isEqualTo(302);
            assertThat(response.getHeaders().get(HttpHeaders.LOCATION).stream().iterator())
                .hasNext();
            assertThat(response.getHeaders().get(HttpHeaders.LOCATION).stream().iterator().next()).
                startsWith(REDIRECT_URI_E_REZEPT_APP);
        });
        idpClient.login(egkUserIdentity);
    }
}
