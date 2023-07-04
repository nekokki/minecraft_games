package plugin.minecraftgames.mapper.data;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class ShinkeisuijakuScore {

  private String playerName;
  private int score;
  private LocalDateTime registeredAt;

  public ShinkeisuijakuScore(String playerName, int score) {
    this.playerName = playerName;
    this.score = score;
  }

}
