import { describe, expect, it, vi } from 'vitest'
import { clearAndFocusStart, type QuillLike } from '../src/utils/quillFocus'

describe('clearAndFocusStart', () => {
  it('clears content and focuses selection at index 0', () => {
    const quill: QuillLike = {
      setText: vi.fn(),
      setSelection: vi.fn(),
      focus: vi.fn()
    }
    clearAndFocusStart(quill)
    expect(quill.setText).toHaveBeenCalledWith('')
    expect(quill.setSelection).toHaveBeenCalledWith(0, 0, 'silent')
    expect(quill.focus).toHaveBeenCalled()
  })
})

