package cn.bitlinks.ems.module.acquisition.dal.mysql.partition;

import cn.bitlinks.ems.module.acquisition.service.partition.PartitionDayRange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PartitionMapper {
//    void addEarlyPartition(@Param("tableName") String tableName,
//                           @Param("cutoffDate") String cutoffDate);
//
//    void addOnePartition(@Param("tableName") String tableName,
//                         @Param("partitionName") String partitionName,
//                         @Param("lessThanDate") String lessThanDate);

    /**
     * 批量新增分区
     * @param tableName
     * @param partitions
     */
    void batchAddPartitions(@Param("tableName") String tableName, @Param("partitions")List<PartitionDayRange> partitions);
}
