package plugin.minecraftgames.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import plugin.minecraftgames.mapper.data.ShinkeisuijakuScore;

public interface ShinkeisuijakuScoreMapper {

  @Select("select * from shinkeisuijaku")
  List<ShinkeisuijakuScore> selectList();

  @Insert("insert into shinkeisuijaku(player_name, score, registered_at) values (#{playerName}, #{score}, now())")
  int insert(ShinkeisuijakuScore shinkeisuijaku);

}
