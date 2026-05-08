export type QuillLike = {
  setText: (text: string) => void
  setSelection: (index: number, length: number, source?: string) => void
  focus: () => void
}

export function clearAndFocusStart(quill: QuillLike) {
  quill.setText('')
  quill.setSelection(0, 0, 'silent')
  quill.focus()
}

