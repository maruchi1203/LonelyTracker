import { useEffect, useState } from "react";
import { deleteSchedule, fetchSchedule, updateSchedule } from "../../api/schedules";
import {
  formToCreateRequest,
  formValidationError,
  formFromDetail,
  type FormVariant,
  type ScheduleForm,
} from "../../domain/scheduleForm";
import ScheduleFields, { type ParentOption } from "./ScheduleFields";

interface Props {
  id: number;
  knownTags: string[];
  parentOptions: ParentOption[];
  variant?: FormVariant;
  showTwoMinute?: boolean;
  onClose: () => void;
  /** 저장이나 삭제가 끝난 뒤. 부른 쪽이 목록을 다시 읽는다 */
  onSaved: () => void;
}

const BUTTON =
  "rounded-md px-4 py-2 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50";

/** 일정 하나를 고치거나 지운다. 지금 값을 서버에서 읽어 폼에 띄운다 */
export default function ScheduleEditModal({
  id,
  knownTags,
  parentOptions,
  variant = "list",
  showTwoMinute,
  onClose,
  onSaved,
}: Props) {
  const [form, setForm] = useState<ScheduleForm | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const fail = (e: unknown, fallback: string) =>
    setError(e instanceof Error ? e.message : fallback);

  useEffect(() => {
    const aborter = new AbortController();

    void fetchSchedule(id, aborter.signal)
      .then((detail) => setForm(formFromDetail(detail)))
      .catch((e: unknown) => {
        // 중간에 닫으면 취소된 요청이라 알릴 것이 없다
        if (!aborter.signal.aborted) fail(e, "일정을 불러오지 못했습니다");
      });

    return () => aborter.abort();
  }, [id]);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  const problem = form ? formValidationError(form, variant) : null;

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form || problem) return;

    setBusy(true);
    setError(null);
    try {
      // 폼의 모든 값을 실어 보낸다. 뺀 칸은 지워진다
      await updateSchedule(id, formToCreateRequest(form));
      onSaved();
      onClose();
    } catch (err) {
      fail(err, "저장하지 못했습니다");
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm("이 항목을 지울까요? 되돌릴 수 없습니다.")) return;

    setBusy(true);
    setError(null);
    try {
      // 리스트에는 습관이 없어 범위가 갈리지 않는다
      await deleteSchedule(id, "ALL");
      onSaved();
      onClose();
    } catch (err) {
      fail(err, "지우지 못했습니다");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 p-4"
      onPointerDown={(e) => {
        // 패널 안을 눌렀을 때는 닫지 않는다
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label="일정 수정"
        className="flex max-h-[85vh] w-[30rem] max-w-full flex-col gap-4 overflow-y-auto rounded-2xl border border-slate-200 bg-white p-5 shadow-xl"
      >
        <h3 className="font-semibold text-slate-800">일정 수정</h3>

        {error && (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
            {error}
          </p>
        )}

        {form === null ? (
          <p className="py-8 text-center text-sm text-slate-400">
            불러오는 중입니다…
          </p>
        ) : (
          <form className="flex flex-col gap-3.5" onSubmit={handleSave}>
            <ScheduleFields
              value={form}
              onChange={(patch) =>
                setForm((prev) => (prev === null ? prev : { ...prev, ...patch }))
              }
              knownTags={knownTags}
              idPrefix={`edit-${id}`}
              variant={variant}
              parentOptions={parentOptions}
              showTwoMinute={showTwoMinute}
            />

            {problem && <p className="text-sm text-red-600">{problem}</p>}

            <div className="flex items-center justify-between gap-2">
              <button
                type="button"
                onClick={() => void handleDelete()}
                disabled={busy}
                className={`${BUTTON} text-red-600 hover:bg-red-50`}
              >
                삭제
              </button>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={onClose}
                  disabled={busy}
                  className={`${BUTTON} text-slate-600 hover:bg-slate-100`}
                >
                  취소
                </button>
                <button
                  type="submit"
                  disabled={busy || problem !== null}
                  className={`${BUTTON} bg-brand-500 text-white hover:bg-brand-600`}
                >
                  저장
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
