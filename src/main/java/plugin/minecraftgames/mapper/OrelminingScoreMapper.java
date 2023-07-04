package plugin.minecraftgames.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import plugin.minecraftgames.mapper.data.OrelMiningScore;

public interface OrelminingScoreMapper {

  @Select("select * from orelmining")
  List<OrelMiningScore> selectList();

  @Insert("insert into orelmining(player_name, score, registered_at) values (#{playerName}, #{score}, now())")
  int insert(OrelMiningScore orelmining);

}
