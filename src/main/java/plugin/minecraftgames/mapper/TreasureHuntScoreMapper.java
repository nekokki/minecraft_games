package plugin.minecraftgames.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import plugin.minecraftgames.mapper.data.TreasureHuntScore;

public interface TreasureHuntScoreMapper {

  @Select("select * from treasurehunt")
  List<TreasureHuntScore> selectList();

  @Insert("insert into treasurehunt(player_name, score, registered_at) values (#{playerName}, #{score}, now())")
  int insert(TreasureHuntScore treasurehunt);

}
