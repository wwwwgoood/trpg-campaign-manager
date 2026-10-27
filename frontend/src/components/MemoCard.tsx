import { useState } from "react";
import type { Memo, MemoCategory } from "../types/memo";

interface MemoCardProps {
    memo: Memo
    onEdit: (memo: Memo) => void
    onDelete: (memoId: number) => void
}

export function MemoCard({
    memo,
    onEdit,
    onDelete
}: MemoCardProps) {
    return (

        <div>
            <h2>{memo.name}</h2>
            <p>{memo.description}</p>

            <button onClick={() => onEdit(memo)}>
                编辑
            </button>

            <button onClick={() => onDelete(memo.id)}>
                删除
            </button>
        </div>)
}