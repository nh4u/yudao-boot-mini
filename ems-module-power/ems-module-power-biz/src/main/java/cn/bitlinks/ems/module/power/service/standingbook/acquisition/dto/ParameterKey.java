package cn.bitlinks.ems.module.power.service.standingbook.acquisition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;


@Schema(description = "管理后台 - 台账-数采设置参数标识 VO")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParameterKey {

    @Schema(description = "参数编码")
    private String code;

    @Schema(description = "是否能源数采参数 0自定义数采 1能源数采")
    private Boolean energyFlag;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterKey that = (ParameterKey) o;
        return Objects.equals(code, that.code) && Objects.equals(energyFlag, that.energyFlag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, energyFlag);
    }
}
