package com.example.seata.at.storage.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * TCC（Try-Confirm-Cancel）方式における在庫引当の「凍結（フリーズ）レコード」エンティティ。
 *
 * 用途:
 * - グローバルトランザクション（xid）単位で TRY/CONFIRM/CANCEL の進捗と入力（productId, count）を永続化します。
 * - これにより以下を満たします。
 *   1) 冪等性: 同一xidでの再実行でも状態に応じて一度だけ実行。
 *   2) サスペンション対策: CANCELが先行した場合、後続のTRYを拒否します。
 *   3) 空振り回避（Empty Rollback）: TRYが無くてもCANCELのみでキャンセル済みマーカーを挿入します。
 *
 * 状態遷移:
 * - 0: TRY（予約） → 1: COMMITTED（確定） → または 2: CANCELED（取消）
 */
@TableName("t_storage_tcc_freeze")
public class StorageTccFreeze {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String xid;
    private Long productId;
    private Integer count;
    /**
     * 0: TRY, 1: COMMITTED, 2: CANCELED
     */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getXid() { return xid; }
    public void setXid(String xid) { this.xid = xid; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
