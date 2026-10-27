import { useState } from "react"
import type { CharacterResponse } from "../types/character"

type Props = {
  character: CharacterResponse
  viewerMemberId: number
  onUpdated: () => void
}

export default function EditCharacterForm({
  character,
  viewerMemberId,
  onUpdated
}: Props) {

  const [name, setName] = useState(character.name)
  const [currentHp, setCurrentHp] = useState(character.currentHp ?? 0)
  const [cashCents, setCashCents] = useState(
    character.cashCents.value ?? 0
  )
  const [desire, setDesire] = useState(
    character.desire.value ?? ""
  )

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()

    setLoading(true)
    setError(null)

    try {
      const response = await fetch(
        `http://localhost:8080/api/characters/${character.id}?viewerMemberId=${viewerMemberId}`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            name,
            gender: character.gender,
            nationality: character.nationality,
            occupation: character.occupation,
            identity: character.identity,

            level: character.level,
            rank: character.rank,

            str: character.str,
            dex: character.dex,
            con: character.con,
            intel: character.intel,
            app: character.app,
            edu: character.edu,

            currentHp,

            cashCents,
            debtCents: character.debtCents.value,
            incomeCents: character.incomeCents.value,
            expenseCents: character.expenseCents.value,

            desire,
            fear: character.fear.value,
            secret: character.secret.value
          })
        }
      )

      if (!response.ok) {
        throw new Error("Failed to update character")
      }

      onUpdated()

    } catch (error) {
      if (error instanceof Error) {
        setError(error.message)
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>Edit Character</h2>

      <input
        value={name}
        onChange={event => setName(event.target.value)}
      />

      <input
        type="number"
        value={currentHp}
        onChange={event =>
          setCurrentHp(Number(event.target.value))
        }
      />

      <input
        type="number"
        value={cashCents}
        onChange={event =>
          setCashCents(Number(event.target.value))
        }
      />

      <input
        value={desire}
        onChange={event => setDesire(event.target.value)}
      />

      <button
        type="submit"
        disabled={loading}
      >
        {loading ? "Updating..." : "Update"}
      </button>

      {error && <p>{error}</p>}
    </form>
  )
}