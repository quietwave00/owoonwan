import { PageIntro } from "../../components/common/Ui";

export function AdminCheckinsPage() {
  return (
    <main className="page-shell">
      <section className="surface-card">
        <PageIntro
          eyebrow="Admin"
          title="체크인 조정"
          description="날짜 기준 체크인 상태를 관리합니다."
        />
      </section>
    </main>
  );
}
