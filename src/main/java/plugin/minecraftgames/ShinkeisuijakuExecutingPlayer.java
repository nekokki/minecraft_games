package plugin.minecraftgames;

import java.time.LocalDateTime;

public class ShinkeisuijakuExecutingPlayer {
  private final String playerName;
  private Integer score;
  private final LocalDateTime gameStartTime;

  public ShinkeisuijakuExecutingPlayer(String playerName, Integer score, LocalDateTime gameStartTime) {
    this.playerName = playerName;
    this.score = score;
    this.gameStartTime = gameStartTime;
  }

  public String getPlayerName() {
    return playerName;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  @Override
  public String toString() {
    return "PlayerName: " + playerName + ", Score: " + score + ", Game Start Time: " + gameStartTime;
  }

}
