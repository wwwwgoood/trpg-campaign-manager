import { useEffect, useState } from "react"
import type { CreateMemoRequest, Memo, MemoCategory } from "../types/memo"

interface MemoFormProps {
    editingMemo: Memo | null
    onSubmit: (request: CreateMemoRequest) => Promise<void>
}

export function MemoForm({
    editingMemo, onSubmit
}: MemoFormProps) {
    const [name, setName] = useState("")
    const [category, setCategory] =
        useState<MemoCategory>("CUSTOM")
    const [description, setDescription] = useState("")
    useEffect(() => {
        if (editingMemo !== null) {
            setName(editingMemo.name)
            setCategory(editingMemo.category)
            setDescription(editingMemo.description)
        }
    }, [editingMemo])
    async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault()

        const request: CreateMemoRequest = {
            name,
            category,
            description
        }

        await onSubmit(request)

        setName("")
        setCategory("CUSTOM")
        setDescription("")
    }
    return (
        <div>
            <form onSubmit={handleSubmit}>
                <div>
                    <label>名称：</label>
                    <input
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                    />
                </div>

                <div>
                    <label>分类：</label>
                    <select
                        value={category}
                        onChange={(event) =>
                            setCategory(event.target.value as MemoCategory)
                        }
                    >
                        <option value="NPC">NPC</option>
                        <option value="SECRET">SECRET</option>
                        <option value="LOCATION">LOCATION</option>
                        <option value="ORGANIZATION">ORGANIZATION</option>
                        <option value="ITEM">ITEM</option>
                        <option value="EVENT">EVENT</option>
                        <option value="CUSTOM">CUSTOM</option>
                    </select>
                </div>

                <div>
                    <label>介绍：</label>
                    <textarea
                        value={description}
                        onChange={(event) => setDescription(event.target.value)}
                    />
                </div>

                <button type="submit">
                    创建Memo
                </button>
            </form>
        </div>
    )
}