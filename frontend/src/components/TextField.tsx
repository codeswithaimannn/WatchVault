import { useId, type ComponentProps } from 'react'

interface TextFieldProps extends ComponentProps<'input'> {
  label: string
  error?: string
}

/**
 * Labelled input with its error message underneath. The label and error are linked to the
 * input (htmlFor, aria-describedby) so screen readers announce them.
 * In React 19, `ref` is a normal prop, so react-hook-form's register() can be spread onto it.
 */
export function TextField({ label, error, id, ...inputProps }: TextFieldProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const errorId = `${inputId}-error`

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-sm font-medium text-zinc-300">
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className="rounded-lg border border-white/10 bg-white/5 px-3 py-2.5 text-zinc-100 transition placeholder:text-zinc-500 focus:border-violet-400/60 focus:bg-white/[0.07] focus:ring-2 focus:ring-violet-500/30 focus:outline-none aria-invalid:border-rose-500/70"
        {...inputProps}
      />
      {error && (
        <p id={errorId} className="text-sm text-rose-300">
          {error}
        </p>
      )}
    </div>
  )
}
