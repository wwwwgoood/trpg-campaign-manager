import {  useEffect, useState } from 'react'
import { getMemos, createMemo, updateMemo, deleteMemo } from '../api/memoApi';
import type { Memo, CreateMemoRequest, MemoCategory } from '../types/memo';
import { MemoCard } from '../components/MemoCard';
import { MemoForm } from '../components/MemoForm';

export function MemoPage(){
    const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [memos, setMemos] = useState<Memo[]>([])
  const [editingMemo, setEditingMemo] = useState<Memo | null>(null)
  const [filterCategory, setFilterCategory] = useState<MemoCategory | "ALL">("ALL")
  const filteredMemos = filterCategory === "ALL" ? memos : memos.filter((memo) => memo.category === filterCategory);
  
  async function handleSave(
  request: CreateMemoRequest
) {
  try {
    if (editingMemo === null) {

      const newMemo = await createMemo(1, request)

      setMemos([
        ...memos,
        newMemo
      ])

    } else {

      const updatedMemo = await updateMemo(
        editingMemo.id,
        request
      )

      setMemos(
        memos.map((memo) =>
          memo.id === updatedMemo.id
            ? updatedMemo
            : memo
        )
      )

      setEditingMemo(null)
    }
  } catch (error) {
    if (error instanceof Error) {
      setError(error.message)
    } else {
      setError("保存Memo时发生未知错误")
    }
    throw error
  }
}

  function handleEdit(memo: Memo) {
    setEditingMemo(memo)
  }

  useEffect(() => {
    async function loadMemos() {
      try {
        const data: Memo[] = await getMemos(1)
        setMemos(data)
      } catch (error) {
        if (error instanceof Error) {
          setError(error.message)
        } else {
          setError("发生未知错误")
        }
      } finally {
        setLoading(false)
      }
    }

    loadMemos()
  }, [])

  if (loading) {
    return <p>正在加载...</p>
  }

  if (error) {
    return <p>加载失败：{error}</p>
  }
  async function handleDelete(memoId: number) {
    try {
      await deleteMemo(memoId)
      setMemos(memos.filter((memo) => memo.id !== memoId))
    }
    catch (error) {
      if (error instanceof Error) {
        setError(error.message)
      } else {
        setError("删除Memo时发生未知错误")
      }
    }

  }

  return (

    <div>
      <h1>TRPG Campaign Manager</h1>

      {filteredMemos.map((memo) => (
        <MemoCard 
        key={memo.id}
        memo={memo}
        onEdit={handleEdit}
        onDelete={handleDelete}
        >

        </MemoCard>

      ))}
      <MemoForm editingMemo={editingMemo} onSubmit={handleSave}></MemoForm>
      <select
        value={filterCategory}
        onChange={(event) =>
          setFilterCategory(
            event.target.value as MemoCategory | "ALL"
          )
        }
      >
        <option value="ALL">全部</option>
        <option value="NPC">NPC</option>
        <option value="SECRET">SECRET</option>
        <option value="LOCATION">LOCATION</option>
        <option value="ORGANIZATION">ORGANIZATION</option>
        <option value="ITEM">ITEM</option>
        <option value="EVENT">EVENT</option>
        <option value="CUSTOM">CUSTOM</option>
      </select>
      
    </div>
  )
}