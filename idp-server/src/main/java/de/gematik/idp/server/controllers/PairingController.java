/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.idp.server.controllers;

import static de.gematik.idp.IdpConstants.PAIRING_ENDPOINT;
import de.gematik.idp.server.RequestAccessToken;
import de.gematik.idp.server.data.PairingDto;
import de.gematik.idp.server.data.PairingList;
import de.gematik.idp.server.services.PairingService;
import de.gematik.idp.server.validation.accessToken.ValidateAccessToken;
import de.gematik.idp.server.validation.clientSystem.ValidateClientSystem;
import de.gematik.idp.token.IdpJwe;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import net.dracoblue.spring.web.mvc.method.annotation.HttpResponseHeader;
import net.dracoblue.spring.web.mvc.method.annotation.HttpResponseHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Transactional
@Valid
@HttpResponseHeaders({
    @HttpResponseHeader(name = "Cache-Control", value = "no-store"),
    @HttpResponseHeader(name = "Pragma", value = "no-cache")
})
public class PairingController {

    private final PairingService pairingService;
    private final RequestAccessToken requestAccessToken;

    @GetMapping(value = PAIRING_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ValidateClientSystem
    @ValidateAccessToken
    public PairingList getAllPairingsForKvnr() {
        return new PairingList(pairingService.validateTokenAndGetPairingList(requestAccessToken.getAccessToken()));
    }

    @DeleteMapping(value = PAIRING_ENDPOINT + "/{key_identifier}")
    @ValidateAccessToken
    @ValidateClientSystem
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteSinglePairing(
        @PathVariable(value = "key_identifier") @NotNull(message = "4001") final String keyIdentifier) {
        pairingService
            .validateTokenAndDeleteSelectedPairing(requestAccessToken.getAccessToken(), keyIdentifier);
    }

    @PostMapping(value = PAIRING_ENDPOINT, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ValidateAccessToken
    @ValidateClientSystem
    public PairingDto insertPairing(
        @RequestParam(value = "encrypted_registration_data", required = false) @NotNull final IdpJwe registrationData) {
        return pairingService
            .validatePairingData(requestAccessToken.getAccessToken(), registrationData);
    }
}
