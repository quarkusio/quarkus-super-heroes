package io.quarkus.sample.superheroes.fight.client;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.FormParam;

import io.vertx.core.json.JsonObject;

import org.jboss.resteasy.reactive.RestForm;

public abstract class WebAuthnResponse {

  /**
   * Corresponds to the JSON {@code id} field
   */
  @FormParam("webAuthnId")
  public String webAuthnId;

  /**
   * Corresponds to the JSON {@code rawId} field
   */
  @FormParam("webAuthnRawId")
  public String webAuthnRawId;

  /**
   * Corresponds to the JSON {@code response.clientDataJSON} field
   */
  @FormParam("webAuthnResponseClientDataJSON")
  public String webAuthnResponseClientDataJSON;

  /**
   * Corresponds to the JSON {@code type} field
   */
  @FormParam("webAuthnType")
  public String webAuthnType;

  /**
   * Turns this form-based structure into its corresponding JSON structure
   */
  public JsonObject toJsonObject() {
    JsonObject response = new JsonObject();
    if (webAuthnResponseClientDataJSON != null)
      response.put("clientDataJSON", webAuthnResponseClientDataJSON);
    toJsonObject(response);
    return new JsonObject()
      .put("id", webAuthnId)
      .put("rawId", webAuthnRawId)
      .put("response", response)
      .put("type", webAuthnType);
  }

  protected abstract void toJsonObject(JsonObject response);

  /**
   * Returns true if any value is set (really looks at the ID only)
   *
   * @return true if any value is set (really looks at the ID only)
   */
  public boolean isSet() {
    return webAuthnId != null && !webAuthnId.isBlank();
  }

  /**
   * Returns true if the id, rawId and type are set, and type is set to "public-key"
   *
   * @return true if this can be passed to the login/register endpoints
   */
  public boolean isValid() {
    return notEmpty(webAuthnId)
      && notEmpty(webAuthnRawId)
      && notEmpty(webAuthnType)
      && webAuthnType.equals("public-key");
  }

  private boolean notEmpty(String value) {
    return value != null && !value.isBlank();
  }
}
