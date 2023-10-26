package io.quarkus.sample.superheroes.narration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import io.quarkus.sample.superheroes.narration.Fight;
import io.quarkus.sample.superheroes.narration.Fight.FightLocation;
import io.quarkus.sample.superheroes.narration.config.NarrationConfig;
import io.quarkus.test.junit.QuarkusMock;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import reactor.core.publisher.Flux;

abstract class OpenAINarrationServiceTestsBase<T extends OpenAINarrationServiceBase> {
  protected static final String HERO_NAME = "Super Baguette";
  protected static final int HERO_LEVEL = 42;
  protected static final String HERO_POWERS = "Eats baguette in less than a second";
  protected static final String HERO_TEAM_NAME = "heroes";
  protected static final String VILLAIN_NAME = "Super Chocolatine";
  protected static final int VILLAIN_LEVEL = 43;
  protected static final String VILLAIN_POWERS = "Transforms chocolatine into pain au chocolat";
  protected static final String VILLAIN_TEAM_NAME = "villains";

  protected static final Fight FIGHT = new Fight(
    VILLAIN_NAME,
    VILLAIN_LEVEL,
    VILLAIN_POWERS,
    HERO_NAME,
    HERO_LEVEL,
    HERO_POWERS,
    VILLAIN_TEAM_NAME,
    HERO_TEAM_NAME,
    new FightLocation(
      "Gotham City",
      "An American city rife with corruption and crime, the home of its iconic protector Batman."
    )
  );

  protected static final String PROMPT = """
    ACT LIKE YOU WERE A MARVEL COMICS WRITER, EXPERT IN ALL SORTS OF SUPER HEROES AND SUPER VILLAINS.
    NARRATE THE FIGHT BETWEEN A SUPER HERO AND A SUPER VILLAIN.
    DURING THE NARRATION DON'T REPEAT "super hero" OR "super villain" WE KNOW WHO IS WHO.
    WRITE 4 PARAGRAPHS MAXIMUM.
    
    THE NARRATION MUST BE:
    - G RATED
    - WORKPLACE/FAMILY SAFE
    NO SEXISM, RACISM OR OTHER BIAS/BIGOTRY
    
    BE CREATIVE.
    
    HERE IS THE DATA YOU WILL USE FOR THE WINNER:
    
    +++++
    name:   Super Chocolatine
    powers: Transforms chocolatine into pain au chocolat
    level:  43
    +++++
                                           
    HERE IS THE DATA YOU WILL USE FOR THE LOSER:
                                           
    +++++
    name:   Super Baguette
    powers: Eats baguette in less than a second
    level:  42
    +++++
    
    HERE IS THE DATA YOU WILL USE FOR THE FIGHT:
    
    +++++
    Super Chocolatine WHO IS A villains HAS WON THE FIGHT AGAINST Super Baguette WHO IS A heroes.
    
    THE FIGHT TOOK PLACE IN Gotham City WHICH CAN BE DESCRIBED AS An American city rife with corruption and crime, the home of its iconic protector Batman.
    +++++
                                       """;

  protected static final ArgumentMatcher<ChatCompletionsOptions> COMPLETIONS_OPTIONS_MATCHER = completionOptions ->
    completionOptions.getMessages().stream()
      .map(ChatMessage::getContent)
      .collect(Collectors.joining("\n"))
      .contains(PROMPT);

  @Inject
  NarrationConfig narrationConfig;

  @Inject
  Instance<OpenAIAsyncClient> openAIAsyncClientInstance;

  OpenAIAsyncClient openAIAsyncClient = mock(OpenAIAsyncClient.class);

  @BeforeEach
  public void setupMocks() {
    QuarkusMock.installMockForInstance(this.openAIAsyncClient, this.openAIAsyncClientInstance.get());
  }

  protected abstract T getNarrationService();
  protected abstract String getModelId();
  protected abstract Duration getExpectedOperationTimeout();

  @Test
  public void narrateSuccess() {
    var expectedNarration = String.format("%s won over %s", HERO_NAME, VILLAIN_NAME);
    var narrationService = getNarrationService();

    doReturn(Flux.just(mockCompletion(expectedNarration)))
      .when(this.openAIAsyncClient)
      .getChatCompletionsStream(
        eq(getModelId()),
        argThat(COMPLETIONS_OPTIONS_MATCHER)
      );

    var narration = narrationService.narrate(FIGHT)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(narration)
      .isNotNull()
      .isEqualTo(expectedNarration);

    verify(narrationService).narrate(eq(FIGHT));
    verify(narrationService, never()).fallbackNarrate(eq(FIGHT));
  }

  @Test
  public void narrateFallback() {
    doThrow(new RuntimeException())
      .when(this.openAIAsyncClient)
      .getChatCompletionsStream(
        eq(getModelId()),
        argThat(COMPLETIONS_OPTIONS_MATCHER)
      );

    var narrationService = getNarrationService();

    var narration = narrationService.narrate(FIGHT)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(Duration.ofSeconds(10))
      .getItem();

    assertThat(narration)
      .isNotNull()
      .isEqualTo(this.narrationConfig.fallbackNarration());

    verify(narrationService).narrate(eq(FIGHT));
    verify(narrationService).fallbackNarrate(eq(FIGHT));
  }

  @Test
  public void narrateTimeout() {
    var timeout = getExpectedOperationTimeout();
    var timeoutPlusOneSecond = timeout.plusSeconds(1);
    var threeTimes = timeoutPlusOneSecond.multipliedBy(3);

    doReturn(
      Flux.just(mockCompletion("Superman beat Spiderman"))
        .delayElements(timeoutPlusOneSecond)
    )
      .when(this.openAIAsyncClient)
      .getChatCompletionsStream(
        eq(getModelId()),
        argThat(COMPLETIONS_OPTIONS_MATCHER)
      );

    var narrationService = getNarrationService();

    var narration = narrationService.narrate(FIGHT)
      .subscribe().withSubscriber(UniAssertSubscriber.create())
      .assertSubscribed()
      .awaitItem(threeTimes)
      .getItem();

    assertThat(narration)
      .isNotNull()
      .isEqualTo(this.narrationConfig.fallbackNarration());

    verify(narrationService).narrate(eq(FIGHT));
    verify(narrationService).fallbackNarrate(eq(FIGHT));
  }

  private static ChatCompletions mockCompletion(String expected) {
    var choices = Stream.of(
        expected.substring(0, expected.length() / 2),
        expected.substring(expected.length() / 2)
      )
      .map(r -> {
        var message = mock(ChatMessage.class);
        var choice = mock(ChatChoice.class);

        when(message.getContent()).thenReturn(r);
        when(choice.getDelta()).thenReturn(message);
        when(choice.getMessage()).thenReturn(message);

        return choice;
      })
      .toList();

    var completions = mock(ChatCompletions.class);

    when(completions.getChoices()).thenReturn(choices);
    when(completions.getId()).thenReturn(UUID.randomUUID().toString());

    return completions;
  }
}
