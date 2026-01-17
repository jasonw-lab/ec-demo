package com.demo.ec.account.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TCC（Try-Confirm-Cancel）方式におけるアカウント減算の「凍結（フリーズ）レコード」エンティティ。
 *
 * 用途:
 * - グローバルトランザクション（xid）単位で TRY/CONFIRM/CANCEL の進捗と入力（userId, amount）を永続化します。
 * - これにより以下を満たします。
 *   1) 冪等性: 同一xidでの再実行でも状態に応じて一度だけ実行。
 *   2) サスペンション対策: CANCELが先に来た場合、後続のTRYを拒否します。
 *   3) 空振り回避（Empty Rollback）: TRYが届かずCANCELだけ到着しても、キャンセル済みマーカーを挿入して整合性を保ちます。
 *
 * 状態遷移:
 * - 0: TRY（予約） → 1: COMMITTED（確定） → または 2: CANCELED（取消）
 */
@TableName("t_account_tcc_freeze")
public class AccountTccFreeze {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String xid;
    private Long userId;
    private BigDecimal amount;
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

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
