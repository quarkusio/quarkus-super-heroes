package io.quarkus.sample.superheroes.fight.client;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.FormParam;

import io.vertx.core.json.JsonObject;

//Copied from quarkus code
public class WebAuthnRegisterResponse extends WebAuthnResponse {
  /**
   * Corresponds to the JSON {@code response.attestationObject} field
   */
  @FormParam("webAuthnResponseAttestationObject")
  public String webAuthnResponseAttestationObject;

  @Override
  protected void toJsonObject(JsonObject response) {
    if (webAuthnResponseAttestationObject != null)
      response.put("attestationObject", webAuthnResponseAttestationObject);
  }
}
