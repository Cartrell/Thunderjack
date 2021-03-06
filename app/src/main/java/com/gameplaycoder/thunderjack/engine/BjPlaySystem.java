package com.gameplaycoder.thunderjack.engine;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.gameplaycoder.thunderjack.R;
import com.gameplaycoder.thunderjack.cards.Card;
import com.gameplaycoder.thunderjack.cards.CardValues;
import com.gameplaycoder.thunderjack.cards.Deck;
import com.gameplaycoder.thunderjack.engine.debug.DebugCardKeys;
import com.gameplaycoder.thunderjack.engine.sound.BjSoundSystem;
import com.gameplaycoder.thunderjack.engine.thunderjackvfx.ThunderjackVfx;
import com.gameplaycoder.thunderjack.layouts.BetAndCreditsTexts;
import com.gameplaycoder.thunderjack.players.BasePlayerData;
import com.gameplaycoder.thunderjack.players.PlayerData;
import com.gameplaycoder.thunderjack.players.PlayerIds;
import com.gameplaycoder.thunderjack.utils.CalculateScore;
import com.gameplaycoder.thunderjack.utils.CardsMatcher;
import com.gameplaycoder.thunderjack.utils.cardsTweener.CardsTweener;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

class BjPlaySystem {
  //=========================================================================
  // members
  //=========================================================================
  private final String LOG_TAG = BjPlaySystem.class.getName();

  private ArrayList<PlayerIds> m_playerIdsOrder;
  private IBjEngine m_engine;
  private BjPlayStates m_state;
  private BjPlaySettingsManager m_playSettingsManager;

  private CardsTweener m_cardsTweener;
  private CardsTweener.OnCompletedListener m_cardsTweenerCompleteListener;
  private CardsTweener.OnUnitStartedListener m_cardsTweenerUnitStartedListener;

  private CardsMatcher m_cardsMatcher;
  private BjSoundSystem.OnSoundCompleteListener m_onDealerBustSoundCompleteListener;
  private PlayerIds m_turnPlayerId;
  private int m_baseCardImageChildIndex;
  private int m_nextCardImageChildIndex;
  private int m_totalCreditsWonOnRound;
  private boolean m_wereAcesSplit;
  private boolean m_wasThunderjackRound;
  private ThunderjackVfx m_thunderjackVfx;

  //=========================================================================
  // package-private
  //=========================================================================

  //-------------------------------------------------------------------------
  // ctor
  //-------------------------------------------------------------------------
  BjPlaySystem(IBjEngine engine) {
    m_engine = engine;
    m_playerIdsOrder  = new ArrayList<>();

    initCardsTweener();

    m_cardsMatcher = new CardsMatcher(m_engine.getIntegerResource(R.integer.blackjackPoints),
      m_engine.getIntegerResource(R.integer.maxCardsPerHand));
    m_baseCardImageChildIndex = m_engine.getIndexOf(m_engine.getLayoutComps().settingsButtonAndDeck.getLayout());
    m_playSettingsManager = new BjPlaySettingsManager(m_engine);
    initGameButtons();
    initDealerBustSoundCompleteListener();
  }

  //-------------------------------------------------------------------------
  // begin
  //-------------------------------------------------------------------------
  void begin() {
    m_state = BjPlayStates.DEAL_CARDS;
    m_nextCardImageChildIndex = m_baseCardImageChildIndex + 1;
    m_totalCreditsWonOnRound = 0;

    TextView txtTotalWon = m_engine.getLayoutComps().betAndCreditsTexts.txtTotalWon;
    if (txtTotalWon != null) {
      m_engine.showView(txtTotalWon, false);
    }

    initPlayers();
    dealCards();
  }

  //=========================================================================
  // private
  //=========================================================================

  //-------------------------------------------------------------------------
  // aceUpSleeve
  //-------------------------------------------------------------------------
  private void aceUpSleeve() {
    Deck deck = m_engine.getDeck();
    int targetIndex = 0;
    for (String cardKey : DebugCardKeys.DEBUG_CARD_KEYS) {
      deck.setCardAt(cardKey, targetIndex++);
    }
  }

  //-------------------------------------------------------------------------
  // arePlayersLeftVsDealer
  //-------------------------------------------------------------------------
  private boolean arePlayersLeftVsDealer() {
    for (PlayerIds playerId : PlayerIds.values()) {
      if (isDealerId(playerId)) {
        continue;
      }

      PlayerData playerData = (PlayerData)getPlayerData(playerId);
      if (playerData.isVsDealer()) {
        return(true);
      }
    }

    return(false);
  }

  //-------------------------------------------------------------------------
  // beginDealerBlackjack
  //-------------------------------------------------------------------------
  private void beginDealerBlackjack() {
    BasePlayerData dealerData = m_engine.getDealerData();
    dealerData.setBlackjack();
    dealerData.setResultImage(R.drawable.result_label_blackjack);
    dealerData.setResultImageVisible(true);

    revealDealerSecondCard();

    //determine any players who got tj or bj
    ArrayList<PlayerIds>bjPlayerIds = new ArrayList<>();
    ArrayList<PlayerIds>tjPlayerIds = new ArrayList<>();
    getBlackjackPlayers(bjPlayerIds, tjPlayerIds);

    if (tjPlayerIds.size() == 1) {
      //this player has tj and beats the dealer's bj
      PlayerIds playerId = tjPlayerIds.get(0);
      beginPlayerThunderjack(playerId);
      playThunderjackSound();
      m_playerIdsOrder.remove(playerId);
    } else if (tjPlayerIds.size() > 1) {
      //more than one player got tj, so they all cancel each other out, resulting in pushes
      for (PlayerIds playerId : bjPlayerIds) {
        //these players have bj, and result in a push vs the dealer's bj
        beginPlayerPush(playerId);
        m_playerIdsOrder.remove(playerId);
      }
    }

    for (PlayerIds playerId : bjPlayerIds) {
      //these players have bj, and result in a push vs the dealer's bj
      beginPlayerBlackjackPush(playerId);
      m_playerIdsOrder.remove(playerId);
    }

    //any remaining players vs dealer have neither bj nor tj, and lost
    for (PlayerIds playerId : m_playerIdsOrder) {
      beginPlayerLost(playerId);
    }

    endRound();
  }

  //-------------------------------------------------------------------------
  // beginDealerBust
  //-------------------------------------------------------------------------
  private void beginDealerBust() {
    BasePlayerData dealerData = getTurnPlayerData();
    dealerData.setBust();
    dealerData.setResultImage(R.drawable.result_label_bust);
    dealerData.setResultImageVisible(true);

    //all players eligible vs dealer automatically win
    for (PlayerIds playerId : PlayerIds.values()) {
      if (isDealerId(playerId)) {
        continue; //dealer cant compete with self! c'mon now! :P
      }

      PlayerData playerData = (PlayerData)getPlayerData(playerId);
      if (playerData.isVsDealer()) {
        beginPlayerWon(playerId);
      }
    }

    m_engine.getSoundSystem().playDealerBust(m_onDealerBustSoundCompleteListener);
  }

  //-------------------------------------------------------------------------
  // beginDealerStand
  //-------------------------------------------------------------------------
  private void beginDealerStand() {
    beginNextTurnPlayer();
  }

  //-------------------------------------------------------------------------
  // beginDealerTurn
  //-------------------------------------------------------------------------
  private void beginDealerTurn() {
    boolean shouldRevealSecondCard = !PlayerIds.DEALER.equals(m_turnPlayerId);
    m_turnPlayerId = PlayerIds.DEALER;

    if (shouldRevealSecondCard) {
      revealDealerSecondCard();
    }

    if (!arePlayersLeftVsDealer()) {
      beginDealerStand();
      return;
    }

    if (doesTurnPlayerHaveMaxCards()) {
      if (shouldDealerStand()) {
        beginDealerStand();
      } else {
        beginDealerBust();
      }
    } else if (shouldDealerStand()) {
      beginDealerStand();
    } else {
      beginHit();
    }
  }

  //-------------------------------------------------------------------------
  // beginDouble
  //-------------------------------------------------------------------------
  private void beginDouble() {
    PlayerData playerData = (PlayerData)getTurnPlayerData();
    int playerBetValue = playerData.getBetValue();
    m_engine.setCredits(-playerBetValue, true);
    playerData.setDoubleDown();
    m_engine.setPlayerBet(playerData.getId(), playerBetValue * 2, false);

    m_state = BjPlayStates.DOUBLE;
    drawCard(m_turnPlayerId, true, 0, true);

    m_engine.getSoundSystem().playDoubleDown();
  }

  //-------------------------------------------------------------------------
  // beginHit
  //-------------------------------------------------------------------------
  private void beginHit() {
    m_state = BjPlayStates.HIT;
    drawCard(m_turnPlayerId, true, 0, true);
    m_engine.getSoundSystem().playHit();
  }

  //-------------------------------------------------------------------------
  // beginNextTurnPlayer
  //-------------------------------------------------------------------------
  private void beginNextTurnPlayer() {
    hideTurnPlayerIndicator();

    if (m_playerIdsOrder.size() > 0) {
      beginUserPlayerTurn();
    } else {
      //if the turn player is the dealer, then end the round
      //otherwise, make the dealer the turn player
      if (isDealersTurn()) {
        endRound();
      } else {
        beginDealerTurn();
      }
    }
  }

  //-------------------------------------------------------------------------
  // beginPlayerBlackjack
  //-------------------------------------------------------------------------
  private void beginPlayerBlackjack(PlayerIds playerId) {
    PlayerData playerData = (PlayerData)getPlayerData(playerId);
    playerData.setBlackjack();
    playerData.setResultImage(R.drawable.anim_result_label_blackjack);
    playerData.setResultImageVisible(true);
    playerData.setBetValueVisible(false);

    int creditsWon = calcCreditsWon(playerId, m_engine.getStringResource(R.string.winRatioBlackjack));
    playerData.setAmountWonValue(creditsWon);
    playerData.setAmountWonValueVisible(true);
    m_totalCreditsWonOnRound += creditsWon;
  }

  //-------------------------------------------------------------------------
  // beginPlayerBlackjackPush
  //-------------------------------------------------------------------------
  private void beginPlayerBlackjackPush(PlayerIds playerId) {
    PlayerData playerData = (PlayerData)getPlayerData(playerId);
    playerData.setBlackjack();
    beginPlayerPush(playerId);
  }

  //-------------------------------------------------------------------------
  // beginPlayerBlitzWin
  //-------------------------------------------------------------------------
  private void beginPlayerBlitzWin() {
    PlayerData playerData = (PlayerData)getTurnPlayerData();
    playerData.setBlitzWin();
    playerData.setResultImage(R.drawable.anim_result_label_blitz);
    playerData.setResultImageVisible(true);
    playerData.setBetValueVisible(false);

    int creditsWon = calcCreditsWon(m_turnPlayerId, m_engine.getStringResource(R.string.winRatioBlitz));
    playerData.setAmountWonValue(creditsWon);
    playerData.setAmountWonValueVisible(true);
    m_totalCreditsWonOnRound += creditsWon;

    playBjBlitzSound();
    beginNextTurnPlayer();
  }

  //-------------------------------------------------------------------------
  // beginPlayerLost
  //-------------------------------------------------------------------------
  private void beginPlayerLost(PlayerIds playerId) {
    if (isDealerId(playerId)) {
      return; //this function for non-dealer players only
    }

    PlayerData playerData = (PlayerData)getPlayerData(playerId);
    playerData.hideBetChips();
    playerData.setBetValueVisible(false);
  }

  //-------------------------------------------------------------------------
  // beginPlayerPush
  //-------------------------------------------------------------------------
  private void beginPlayerPush(PlayerIds playerId) {
    PlayerData playerData = (PlayerData)getPlayerData(playerId);
    playerData.setIsPush();
    playerData.setResultImage(R.drawable.result_label_push);
    playerData.setResultImageVisible(true);
    playerData.setBetValueVisible(false);

    int creditsWon = calcCreditsWon(playerId, m_engine.getStringResource(R.string.winRatioPush));
    playerData.setAmountWonValue(creditsWon);
    playerData.setAmountWonValueVisible(true);
    m_totalCreditsWonOnRound += creditsWon;
  }

  //-------------------------------------------------------------------------
  // beginPlayerStand
  //-------------------------------------------------------------------------
  private void beginPlayerStand() {
    m_engine.getSoundSystem().playStand();
    beginNextTurnPlayer();
  }

  //-------------------------------------------------------------------------
  // beginPlayerThunderjack
  //-------------------------------------------------------------------------
  private void beginPlayerThunderjack(PlayerIds playerId) {
    PlayerData playerData = (PlayerData)getPlayerData(playerId);
    playerData.setBlackjack();
    playerData.setThunderjack();
    playerData.setResultImage(R.drawable.anim_result_label_thunderjack);
    playerData.setResultImageVisible(true);
    playerData.setBetValueVisible(false);

    int creditsWon = calcCreditsWon(playerId, m_engine.getStringResource(R.string.winRatioThunderjack));
    playerData.setAmountWonValue(creditsWon);
    playerData.setAmountWonValueVisible(true);
    m_totalCreditsWonOnRound += creditsWon;

    if (m_thunderjackVfx == null) {
      m_thunderjackVfx = new ThunderjackVfx(m_engine);
    }

    m_thunderjackVfx.begin(playerId);

    m_wasThunderjackRound = true;
  }

  //-------------------------------------------------------------------------
  // beginPlayerThunderjackPush
  //-------------------------------------------------------------------------
  private void beginPlayerThunderjackPush(PlayerIds playerId) {
    PlayerData playerData = (PlayerData)getPlayerData(playerId);
    playerData.setBlackjack();
    playerData.setThunderjack();
    beginPlayerPush(playerId);
  }

  //-------------------------------------------------------------------------
  // beginPlayerWon
  //-------------------------------------------------------------------------
  private void beginPlayerWon(PlayerIds playerId) {
    PlayerData playerData = (PlayerData)getPlayerData(playerId);
    playerData.setNormalWin();
    playerData.setResultImage(R.drawable.result_label_win);
    playerData.setResultImageVisible(true);
    playerData.setBetValueVisible(false);

    int creditsWon = calcCreditsWon(playerId, m_engine.getStringResource(R.string.winRatioNormal));
    playerData.setAmountWonValue(creditsWon);
    playerData.setAmountWonValueVisible(true);
    m_totalCreditsWonOnRound += creditsWon;
  }

  //-------------------------------------------------------------------------
  // beginRoundStart
  //-------------------------------------------------------------------------
  private void beginRoundStart() {
    updatePlayersPoints();

    if (m_cardsMatcher.doesPlayerHaveBlackjack(getPlayerData(PlayerIds.DEALER), m_engine)) {
      beginDealerBlackjack();
    } else {
      boolean isEndOfRoundEffect = checkPlayersForBlackjack();
      if (isEndOfRoundEffect) {
        endRound();
      } else {
        beginNextTurnPlayer();
      }
    }
  }

  //-------------------------------------------------------------------------
  // beginSplit
  //-------------------------------------------------------------------------
  private void beginSplit() {
    PlayerData turnPlayerData = (PlayerData)getTurnPlayerData();
    PlayerData splitPlayerData = getSplitPlayerData(m_turnPlayerId);
    if (splitPlayerData == null) {
      return; //sanity check
    }

    m_state = BjPlayStates.SPLIT;
    turnPlayerData.setSplit();
    splitPlayerData.setSplit();
    m_engine.setCredits(-turnPlayerData.getBetValue(), true);

    m_wereAcesSplit = wereAcesSplit();
    moveTopCardFromTo(turnPlayerData, splitPlayerData);

    m_engine.getSoundSystem().playSplit();
  }

  //-------------------------------------------------------------------------
  // beginSurrender
  //-------------------------------------------------------------------------
  private void beginSurrender() {
    PlayerData playerData = (PlayerData)getTurnPlayerData();
    playerData.setSurrendered();
    playerData.setResultImage(R.drawable.result_label_surrender);
    playerData.setResultImageVisible(true);

    int creditsWon = calcCreditsWon(m_turnPlayerId, m_engine.getStringResource(R.string.winRatioSurrender));
    playerData.setBetValue(creditsWon);
    playerData.setBetValueVisible(true);
    m_totalCreditsWonOnRound += creditsWon;

    m_engine.getSoundSystem().playSurrender();

    beginNextTurnPlayer();
  }

  //-------------------------------------------------------------------------
  // beginTurnPlayerBust
  //-------------------------------------------------------------------------
  private void beginTurnPlayerBust() {
    m_engine.getSoundSystem().playBust();
    BasePlayerData playerData = getTurnPlayerData();
    playerData.setBust();
    playerData.setResultImage(R.drawable.result_label_bust);
    playerData.setResultImageVisible(true);
    beginPlayerLost(m_turnPlayerId);
    beginNextTurnPlayer();
  }

  //-------------------------------------------------------------------------
  // beginUserPlayerTurn
  //-------------------------------------------------------------------------
  private void beginUserPlayerTurn() {
    m_turnPlayerId = m_playerIdsOrder.get(0);
    m_playerIdsOrder.remove(0);
    PlayerData playerData = (PlayerData)getTurnPlayerData();
    playerData.setTurnIndicatorVisible(true);
    showGameButtons();
    m_state = BjPlayStates.PLAYER_MOVE;
  }

  //-------------------------------------------------------------------------
  // calcCreditsWon
  //-------------------------------------------------------------------------
  private int calcCreditsWon(PlayerIds playerId, String winRatioFormat) {
    //win ratio formats are in x:y, for example, if the format is 3:2, credits won would
    // be 3/2, or 1.5 times original bet value

    PlayerData playerData = (PlayerData)getPlayerData(playerId);
    if (winRatioFormat == null) {
      Log.w(LOG_TAG, "calcCreditsWon. Win ratio format is null");
      return(playerData.getBetValue());

    }

    int delimiterIndex = winRatioFormat.indexOf(":");
    if (delimiterIndex == -1) {
      Log.w(LOG_TAG, "calcCreditsWon. Invalid win ratio format: " + winRatioFormat);
      return(playerData.getBetValue());
    }

    Float divisor;
    Float dividend;
    try {
      divisor = Float.valueOf(winRatioFormat.substring(0, delimiterIndex));
      dividend = Float.valueOf(winRatioFormat.substring(delimiterIndex + 1));
    } catch (Exception error) {
      Log.w(LOG_TAG, "calcCreditsWon. Invalid win ratio format: " + winRatioFormat);
      return(playerData.getBetValue());
    }

    float winRatio = divisor / dividend;
    float betValue = (float)playerData.getOrigBetValue();
    float amountWonF = betValue + betValue * winRatio;
    int amountWon = (int)amountWonF;
    if (playerData.getIsDoubleDown() && !playerData.getIsPush()) {
      amountWon *= 2;
    }
    return(amountWon);
  }

  //-------------------------------------------------------------------------
  // getBlackjackPlayers
  //-------------------------------------------------------------------------
  private void getBlackjackPlayers(ArrayList<PlayerIds>bjPlayerIds_out,
  ArrayList<PlayerIds>tjPlayerIds_out) {
    for (PlayerIds playerId : m_playerIdsOrder) {
      BasePlayerData playerData = getPlayerData(playerId);
      if (m_cardsMatcher.doesPlayerHaveThunderjack(playerData, m_engine)) {
        if (tjPlayerIds_out != null) {
          tjPlayerIds_out.add(playerId);
        }
      } else if (m_cardsMatcher.doesPlayerHaveBlackjack(playerData, m_engine)) {
        if (bjPlayerIds_out != null) {
          bjPlayerIds_out.add(playerId);
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  // checkPlayersForBlackjack
  //-------------------------------------------------------------------------
  private boolean checkPlayersForBlackjack() {
    ArrayList<PlayerIds>bjPlayerIds = new ArrayList<>();
    ArrayList<PlayerIds>tjPlayerIds = new ArrayList<>();
    getBlackjackPlayers(bjPlayerIds, tjPlayerIds);
    boolean isEndOfRoundEffect = false;

    if (tjPlayerIds.size() > 1) {
      //more than one player got tj, so all tjs result in a push
      for (PlayerIds playerId : tjPlayerIds) {
        beginPlayerThunderjackPush(playerId);
        m_playerIdsOrder.remove(playerId);
      }
    } else if (tjPlayerIds.size() == 1) {
      //only one player has tj - won the whole thing
      isEndOfRoundEffect = true;
      beginPlayerThunderjack(tjPlayerIds.get(0));
      playThunderjackSound();
    } else if (bjPlayerIds.size() > 0) {
      //handle any players that got bj
      playBjBlitzSound();
      for (PlayerIds playerId : bjPlayerIds) {
        beginPlayerBlackjack(playerId);
        m_playerIdsOrder.remove(playerId);
      }
    }

    return(isEndOfRoundEffect);
  }

  //-------------------------------------------------------------------------
  // compareHands
  //-------------------------------------------------------------------------
  private void compareHands() {
    //compare every eligible player vs dealer
    BasePlayerData dealerData = m_engine.getDealerData();
    if (dealerData.getIsBust()) {
      //dealer busted - no cards to compare
      return;
    }

    revealDealerSecondCard();

    int dealerScore = dealerData.getScore();

    for (PlayerIds playerId : PlayerIds.values()) {
      if (isDealerId(playerId)) {
        continue; //cant compare dealer with itself!
      }

      PlayerData playerData = (PlayerData)getPlayerData(playerId);
      if (!playerData.isVsDealer()) {
        continue; //player is not eligible vs dealer
      }

      int playerScore = playerData.getScore();
      if (playerScore > dealerScore) {
        beginPlayerWon(playerId);
      } else if (playerScore < dealerScore) {
        beginPlayerLost(playerId);
      } else {
        beginPlayerPush(playerId);
      }
    }
  }

  //-------------------------------------------------------------------------
  // dealCards
  //-------------------------------------------------------------------------
  private void dealCards() {
    aceUpSleeve();

    long moveStartDelay = 0;
    final long MOVE_DURATION = m_engine.getIntegerResource(R.integer.cardMoveDuration);
    final int NUM_CARDS_TO_DEAL = 2;
    for (int cardsRound = 0; cardsRound < NUM_CARDS_TO_DEAL; cardsRound++) {
      for (PlayerIds playerId : m_playerIdsOrder) {
        drawCard(playerId, true, moveStartDelay, false);
        moveStartDelay += MOVE_DURATION;
      }

      //only the dealer's first card is face up
      boolean isFaceUp = cardsRound == 0;

      boolean shouldStartAnimation = cardsRound == (NUM_CARDS_TO_DEAL - 1);
      drawCard(PlayerIds.DEALER, isFaceUp, moveStartDelay, shouldStartAnimation);
      moveStartDelay += MOVE_DURATION;
    }
  }

  //-------------------------------------------------------------------------
  // didTurnPlayerBust
  //-------------------------------------------------------------------------
  private boolean didTurnPlayerBust() {
    final int BLACK_JACK = m_engine.getIntegerResource(R.integer.blackjackPoints);
    return(getTurnPlayerData().getScore() > BLACK_JACK);
  }

  //-------------------------------------------------------------------------
  // doesTurnPlayerHaveMaxCards
  //-------------------------------------------------------------------------
  private boolean doesTurnPlayerHaveMaxCards() {
    return(m_cardsMatcher.doesPlayerHaveMaxCards(getTurnPlayerData()));
  }

  //-------------------------------------------------------------------------
  // drawCard
  //-------------------------------------------------------------------------
  private void drawCard(PlayerIds playerId, boolean isFaceUp, long moveStartDelay,
  boolean startAnimation) {
    Card card = m_engine.getDeck().next();
    if (card == null) {
      //sanity check
      Log.w(LOG_TAG, "drawCard. Warning: No cards left in deck...");
      endRound();
      return;
    }

    m_engine.showGameButtons(BjGameButtonFlags.NONE);

    BasePlayerData basePlayerData = getPlayerData(playerId);
    final long MOVE_DURATION = m_engine.getIntegerResource(R.integer.cardMoveDuration);
    card.setFaceUp(isFaceUp);
    basePlayerData.addCard(card, m_cardsTweener, moveStartDelay, MOVE_DURATION, startAnimation,
      m_nextCardImageChildIndex++, true);
  }

  //-------------------------------------------------------------------------
  // endRound
  //-------------------------------------------------------------------------
  private void endRound() {
    m_engine.showGameButtons(BjGameButtonFlags.NONE);
    compareHands();
    presentWonCredits();
    updatePlaySettings();
    playEndRoundSound();
    m_engine.beginRound();
  }

  //-------------------------------------------------------------------------
  // getPlayerData
  //-------------------------------------------------------------------------
  private BasePlayerData getPlayerData(PlayerIds playerId) {
    return(isDealerId(playerId) ? m_engine.getDealerData() : m_engine.getPlayer(playerId));
  }

  //-------------------------------------------------------------------------
  // getSplitPlayerData
  //-------------------------------------------------------------------------
  private PlayerData getSplitPlayerData(PlayerIds playerId) {
    PlayerData splitPlayerData;

    switch (playerId) {
      case LEFT_BOTTOM:
        splitPlayerData = (PlayerData)getPlayerData(PlayerIds.LEFT_TOP);
        break;

      case MIDDLE_BOTTOM:
        splitPlayerData = (PlayerData)getPlayerData(PlayerIds.MIDDLE_TOP);
        break;

      case RIGHT_BOTTOM:
        splitPlayerData = (PlayerData)getPlayerData(PlayerIds.RIGHT_TOP);
        break;

      default:
        splitPlayerData = null;
        break;
    }

    return(splitPlayerData);
  }

  //-------------------------------------------------------------------------
  // getTurnPlayerData
  //-------------------------------------------------------------------------
  private BasePlayerData getTurnPlayerData() {
    return(getPlayerData(m_turnPlayerId));
  }

  //-------------------------------------------------------------------------
  // hideTurnPlayerIndicator
  //-------------------------------------------------------------------------
  private void hideTurnPlayerIndicator() {
    if (m_turnPlayerId == null) {
      return;
    }

    if (!isDealersTurn()) {
      PlayerData playerData = (PlayerData)getTurnPlayerData();
      playerData.setTurnIndicatorVisible(false);
    }
  }

  //-------------------------------------------------------------------------
  // initCardsTweener
  //-------------------------------------------------------------------------
  private void initCardsTweener() {
    m_cardsTweener = new CardsTweener();
    initCardsTweenerCompleteListener();
    initCardsTweenerUnitStartedListener();
  }

  //-------------------------------------------------------------------------
  // initCardsTweenerUnitStartedListener
  //-------------------------------------------------------------------------
  private void initCardsTweenerUnitStartedListener() {
    m_cardsTweenerUnitStartedListener = new CardsTweener.OnUnitStartedListener() {
      //-------------------------------------------------------------------------
      // onStarted
      //-------------------------------------------------------------------------
      @Override
      public void onStarted(CardsTweener cardsTweener, View cardImage, Object customData) {
        cardImage.setVisibility(View.VISIBLE);

        boolean isCardImageBeingDealt = (boolean)customData;
        if (isCardImageBeingDealt) {
          m_engine.getSoundSystem().playDealCard();
        }
      }
    };

    m_cardsTweener.setOnUnitStartedListener(m_cardsTweenerUnitStartedListener);
  }

  //-------------------------------------------------------------------------
  // initCardsTweenerCompleteListener
  //-------------------------------------------------------------------------
  private void initCardsTweenerCompleteListener() {
    m_cardsTweenerCompleteListener = new CardsTweener.OnCompletedListener() {
      @Override
      //-------------------------------------------------------------------------
      // onCompleted
      //-------------------------------------------------------------------------
      public void onCompleted(CardsTweener cardsTweener) {
        updatePlayersPoints();

        switch (m_state) {
          case DEAL_CARDS:
            beginRoundStart();
            break;

          case HIT:
          case DOUBLE:
          case SPLIT:
            resolveCardsAfterHit();
            break;
        }
      }
    };

    m_cardsTweener.setOnCompletedListener(m_cardsTweenerCompleteListener);
  }

  //-------------------------------------------------------------------------
  // initDealerBustSoundCompleteListener
  //-------------------------------------------------------------------------
  private void initDealerBustSoundCompleteListener() {
    m_onDealerBustSoundCompleteListener = new BjSoundSystem.OnSoundCompleteListener() {

      //-------------------------------------------------------------------------
      // onCompleted
      //-------------------------------------------------------------------------
      @Override
      public void onComplete(BjSoundSystem soundSystem) {
        endRound();
      }
    };
  }

  //-------------------------------------------------------------------------
  // initGameButtonDouble
  //-------------------------------------------------------------------------
  private void initGameButtonDouble() {
    m_engine.getLayoutComps().gameButtons.doubleButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        beginDouble();
      }
    });
  }

  //-------------------------------------------------------------------------
  // initGameButtonHit
  //-------------------------------------------------------------------------
  private void initGameButtonHit() {
    m_engine.getLayoutComps().gameButtons.hitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        beginHit();
      }
    });
  }

  //-------------------------------------------------------------------------
  // initGameButtons
  //-------------------------------------------------------------------------
  private void initGameButtons() {
    initGameButtonHit();
    initGameButtonStand();
    initGameButtonDouble();
    initGameButtonSplit();
    initGameButtonSurrender();
  }

  //-------------------------------------------------------------------------
  // initGameButtonSplit
  //-------------------------------------------------------------------------
  private void initGameButtonSplit() {
    m_engine.getLayoutComps().gameButtons.splitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        beginSplit();
      }
    });
  }

  //-------------------------------------------------------------------------
  // initGameButtonStand
  //-------------------------------------------------------------------------
  private void initGameButtonStand() {
    m_engine.getLayoutComps().gameButtons.standButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        beginPlayerStand();
      }
    });
  }

  //-------------------------------------------------------------------------
  // initGameButtonSurrender
  //-------------------------------------------------------------------------
  private void initGameButtonSurrender() {
    m_engine.getLayoutComps().gameButtons.surrenderButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        beginSurrender();
      }
    });
  }

  //-------------------------------------------------------------------------
  // initPlayers
  //-------------------------------------------------------------------------
  private void initPlayers() {
    resetPlayers();
    resetDealer();
    initPlayersOrder();
  }

  //-------------------------------------------------------------------------
  // initPlayersOrder
  //-------------------------------------------------------------------------
  private void initPlayersOrder() {
    m_playerIdsOrder.clear();

    final PlayerIds[] PLAYER_IDS = new PlayerIds[] {
      PlayerIds.RIGHT_BOTTOM,
      PlayerIds.MIDDLE_BOTTOM,
      PlayerIds.LEFT_BOTTOM
    };

    for (PlayerIds playerId : PLAYER_IDS) {
      PlayerData playerData = m_engine.getPlayer(playerId);
      if (playerData.getBetValue() > 0f) {
        m_playerIdsOrder.add(playerId);
      }
    }
  }

  //-------------------------------------------------------------------------
  // isDealerId
  //-------------------------------------------------------------------------
  private boolean isDealerId(PlayerIds playerId) {
    return(PlayerIds.DEALER.equals(playerId));
  }

  //-------------------------------------------------------------------------
  // isDealersTurn
  //-------------------------------------------------------------------------
  private boolean isDealersTurn() {
    return(isDealerId(m_turnPlayerId));
  }

  //-------------------------------------------------------------------------
  // isDoubleAvailable
  //-------------------------------------------------------------------------
  private boolean isDoubleAvailable() {
    PlayerData playerData = (PlayerData)getTurnPlayerData();
    return(
      m_engine.getCredits() >= playerData.getBetValue() &&
      playerData.getNumCards() == 2);
  }

  //-------------------------------------------------------------------------
  // isSplitAvailable
  //-------------------------------------------------------------------------
  private boolean isSplitAvailable() {
    return(m_cardsMatcher.canPlayerSplit((PlayerData)getTurnPlayerData()));
  }

  //-------------------------------------------------------------------------
  // isSurrenderAvailable
  //-------------------------------------------------------------------------
  private boolean isSurrenderAvailable() {
    PlayerData playerData = (PlayerData)getTurnPlayerData();
    return(playerData.getNumCards() == 2);
  }

  //-------------------------------------------------------------------------
  // moveTopCardFromTo
  //-------------------------------------------------------------------------
  private void moveTopCardFromTo(PlayerData sourcePlayerData, PlayerData targetPlayerData) {
    m_engine.showGameButtons(BjGameButtonFlags.NONE);

    final long MOVE_DURATION = m_engine.getIntegerResource(R.integer.cardMoveDuration);
    Card card = sourcePlayerData.popTopCard(m_cardsTweener, 0, MOVE_DURATION, false);
    targetPlayerData.addCard(card, m_cardsTweener, 0, MOVE_DURATION,
      false, -1, false);
    drawCard(sourcePlayerData.getId(), true, MOVE_DURATION, false);
    drawCard(targetPlayerData.getId(), true, MOVE_DURATION * 2, true);
  }

  //-------------------------------------------------------------------------
  // playBjBlitzSound
  //-------------------------------------------------------------------------
  private void playBjBlitzSound() {
    m_engine.getSoundSystem().playBjBlitz();
  }

  //-------------------------------------------------------------------------
  // playEndRoundSound
  //-------------------------------------------------------------------------
  private void playEndRoundSound() {
    if (m_wasThunderjackRound) {
      m_wasThunderjackRound = false;
      return;
    }

    int endCredits = m_engine.getCredits();
    int startCredits = m_engine.getCreditsAtStartOfRound();

    if (endCredits > startCredits) {
      m_engine.getSoundSystem().playWin();
    } else if (endCredits < startCredits) {
      m_engine.getSoundSystem().retractWin();
      if (m_engine.getDealerData().getHasBlackjack()) {
        m_engine.getSoundSystem().playDealerBj();
      }
    }
  }

  //-------------------------------------------------------------------------
  // playThunderjackSound
  //-------------------------------------------------------------------------
  private void playThunderjackSound() {
    m_engine.getSoundSystem().playTj();
  }

  //-------------------------------------------------------------------------
  // presentWonCredits
  //-------------------------------------------------------------------------
  private void presentWonCredits() {
    if (m_totalCreditsWonOnRound == 0) {
      return; //no credits won this round
    }

    m_engine.setCredits(m_totalCreditsWonOnRound, true);

    BetAndCreditsTexts betAndCreditsTexts = m_engine.getLayoutComps().betAndCreditsTexts;
    betAndCreditsTexts.setTotalWon(m_totalCreditsWonOnRound);
    m_engine.showView(betAndCreditsTexts.txtTotalWon, true);

    m_totalCreditsWonOnRound = 0;
  }

  //-------------------------------------------------------------------------
  // resetDealer
  //-------------------------------------------------------------------------
  private void resetDealer() {
    m_engine.getDealerData().resetStatus();
  }

  //-------------------------------------------------------------------------
  // resetPlayers
  //-------------------------------------------------------------------------
  private void resetPlayers() {
    HashMap<PlayerIds, PlayerData> players = m_engine.getPlayers();
    for (Map.Entry entry : players.entrySet()) {
      PlayerData playerData = (PlayerData)entry.getValue();
      playerData.resetStatus();

      if (m_engine.isSplitPlayerId(playerData.getId())) {
        playerData.removeBetChips();
        playerData.setBetValueVisible(false);
      }
    }
  }

  //-------------------------------------------------------------------------
  // resolveCardsAfterHit
  //-------------------------------------------------------------------------
  private void resolveCardsAfterHit() {
    if (isDealersTurn()) {
      resolveDealerCardsAfterHit();
    } else {
      resolvePlayerCardsAfterHit();
    }
  }

  //-------------------------------------------------------------------------
  // resolveDealerCardsAfterHit
  //-------------------------------------------------------------------------
  private void resolveDealerCardsAfterHit() {
    if (didTurnPlayerBust()) {
      beginDealerBust();
      return;
    }

    beginDealerTurn();
  }

  //-------------------------------------------------------------------------
  // resolvePlayerCardsAfterHit
  //-------------------------------------------------------------------------
  private void resolvePlayerCardsAfterHit() {
    if (didTurnPlayerBust()) {
      beginTurnPlayerBust();
      return;
    }

    if (doesTurnPlayerHaveMaxCards()) {
      beginPlayerBlitzWin();
      return;
    }

    if (BjPlayStates.DOUBLE.equals(m_state)) {
      beginNextTurnPlayer();
      return;
    }

    if (BjPlayStates.SPLIT.equals(m_state)) {
      resolvePlayersAfterSplit();
    }

    resumePlayerTurn();
  }

  //-------------------------------------------------------------------------
  // resolvePlayersAfterSplit
  //-------------------------------------------------------------------------
  private void resolvePlayersAfterSplit() {
    updatePlayerPoints(m_turnPlayerId);

    //setup the new split player
    PlayerData splitPlayerData = getSplitPlayerData(m_turnPlayerId);
    if (splitPlayerData == null) {
      //sanity check
      Log.w(LOG_TAG, "resolvePlayersAfterSplit. Unable to obtain split player for ID: " +
        m_turnPlayerId);
      return;
    }

    PlayerData playerData = (PlayerData)getTurnPlayerData();
    PlayerIds splitPlayerId = splitPlayerData.getId();
    m_engine.setPlayerBet(splitPlayerId, playerData.getBetValue(), true);

    m_playerIdsOrder.add(0, splitPlayerId);
    updatePlayerPoints(splitPlayerId);

    if (m_wereAcesSplit) {
      m_wereAcesSplit = false;
      beginPlayerStand();
      beginPlayerStand();
    }

    m_engine.updateBetValue();
  }

  //-------------------------------------------------------------------------
  // resumePlayerTurn
  //-------------------------------------------------------------------------
  private void resumePlayerTurn() {
    if (isDealersTurn()) {
      beginDealerTurn();
    } else {
      showGameButtons();
      m_state = BjPlayStates.PLAYER_MOVE;
    }
  }

  //-------------------------------------------------------------------------
  // revealDealerSecondCard
  //-------------------------------------------------------------------------
  private void revealDealerSecondCard() {
    BasePlayerData dealerData = m_engine.getDealerData();
    dealerData.setCardFaceUp(1, true);
    updatePlayerPoints(PlayerIds.DEALER);
  }

  //-------------------------------------------------------------------------
  // shouldDealerStand
  //-------------------------------------------------------------------------
  private boolean shouldDealerStand() {
    final int DEALER_POINTS_THRESHOLD = m_engine.getIntegerResource(R.integer.dealerPointsThreshold);
    return(getTurnPlayerData().getScore() >= DEALER_POINTS_THRESHOLD);
  }

  //-------------------------------------------------------------------------
  // showGameButtons
  //-------------------------------------------------------------------------
  private void showGameButtons() {
    //Hit and stand are always available. Always.
    EnumSet<BjGameButtonFlags> flags = EnumSet.of(BjGameButtonFlags.HIT, BjGameButtonFlags.STAND);

    if (isSurrenderAvailable()) {
      flags.add(BjGameButtonFlags.SURRENDER);
    }

    if (isSplitAvailable()) {
      flags.add(BjGameButtonFlags.SPLIT);
    }

    if (isDoubleAvailable()) {
      flags.add(BjGameButtonFlags.DOUBLE);
    }

    m_engine.showGameButtons(flags);
  }

  //-------------------------------------------------------------------------
  // updatePlayerPoints
  //-------------------------------------------------------------------------
  private void updatePlayerPoints(PlayerIds playerId) {
    BasePlayerData playerData = getPlayerData(playerId);

    if (!isDealerId(playerId)) {
      PlayerData pdata = (PlayerData)playerData;
      if (pdata.getBetValue() == 0) {
        return; //not an active player
      }
    }

    ArrayList<String> cardKeys = playerData.getFaceUpCardKeys(true);
    int score = CalculateScore.Run(m_engine, cardKeys);
    playerData.setScore(score);
    playerData.setScoreTextVisible(true);
  }

  //-------------------------------------------------------------------------
  // updatePlayersPoints
  //-------------------------------------------------------------------------
  private void updatePlayersPoints() {
    for (PlayerIds playerId : PlayerIds.values()) {
      updatePlayerPoints(playerId);
    }

    updatePlayerPoints(PlayerIds.DEALER);
  }

  //-------------------------------------------------------------------------
  // updatePlaySettings
  //-------------------------------------------------------------------------
  private void updatePlaySettings() {
    m_playSettingsManager.update();
    m_engine.writeSettings();
  }

  //-------------------------------------------------------------------------
  // wereAcesSplit
  //-------------------------------------------------------------------------
  private boolean wereAcesSplit() {
    PlayerData playerData = (PlayerData)getTurnPlayerData();
    Card card = m_engine.getDeck().getCard(playerData.getCardKetAt(0));
    if (!CardValues.ACE.equals(card.getValue())) {
      return(false);
    }

    card = m_engine.getDeck().getCard(playerData.getCardKetAt(1));
    return(CardValues.ACE.equals(card.getValue()));
  }
}