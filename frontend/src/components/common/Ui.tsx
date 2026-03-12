import type {
  ButtonHTMLAttributes,
  ComponentPropsWithoutRef,
  HTMLAttributes,
  InputHTMLAttributes,
  PropsWithChildren,
  ReactNode,
  TextareaHTMLAttributes,
} from "react";

type Tone = "info" | "success" | "warning" | "error" | "loading";

type ButtonVariant = "primary" | "secondary" | "ghost" | "danger";
type ButtonSize = "md" | "sm";

type SurfaceCardProps = PropsWithChildren<
  HTMLAttributes<HTMLDivElement> & {
    tone?: "default" | "gray";
  }
>;

type PageIntroProps = {
  eyebrow: string;
  title: string;
  description?: string;
  actions?: ReactNode;
};

type BadgeProps = PropsWithChildren<
  HTMLAttributes<HTMLSpanElement> & {
    variant?: "primary" | "accent" | "muted";
  }
>;

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  block?: boolean;
};

type FieldProps = {
  label?: string;
  hint?: string;
  error?: string | null;
  children: ReactNode;
};

type TextInputProps = InputHTMLAttributes<HTMLInputElement>;
type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement>;

type StateCardProps = {
  eyebrow: string;
  title: string;
  description?: string;
  tone?: Tone;
  action?: ReactNode;
};

type NoticeProps = {
  title: string;
  tone?: Tone;
};

type ToastItem = {
  id: string;
  title: string;
  description: string;
  tone?: Tone;
};

type TableCardProps = PropsWithChildren<{
  eyebrow: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}>;

type SidePanelProps = PropsWithChildren<{
  eyebrow: string;
  title: string;
  description?: string;
  footer?: ReactNode;
  tone?: "default" | "gray";
}>;

type CalendarDay = {
  dateLabel: string;
  statusLabel: string;
  isPresent?: boolean;
  isToday?: boolean;
  isMuted?: boolean;
};

type CalendarCardProps = {
  eyebrow: string;
  title: string;
  description?: string;
  weekdays: string[];
  days: CalendarDay[];
};

const toneIconMap: Record<Tone, string> = {
  info: "i",
  success: "v",
  warning: "!",
  error: "x",
  loading: "...",
};

function cx(...classNames: Array<string | false | null | undefined>) {
  return classNames.filter(Boolean).join(" ");
}

export function SurfaceCard({ children, className, tone = "default", ...props }: SurfaceCardProps) {
  return <section className={cx("ui-card", tone === "gray" && "ui-card--gray", className)} {...props}>{children}</section>;
}

export function PageIntro({ eyebrow, title, description, actions }: PageIntroProps) {
  return (
    <div className={cx("page-intro", Boolean(actions) && "page-intro--split")}>
      <div>
        <p className="section-kicker">{eyebrow}</p>
        <h2 className="page-intro__title">{title}</h2>
        {description ? <p className="ui-card__description">{description}</p> : null}
      </div>
      {actions ? <div className="page-intro__actions">{actions}</div> : null}
    </div>
  );
}

export function Badge({ children, className, variant = "muted", ...props }: BadgeProps) {
  return <span className={cx("ui-badge", `ui-badge--${variant}`, className)} {...props}>{children}</span>;
}

export function Button({ children, className, variant = "primary", size = "md", block = false, ...props }: ButtonProps) {
  return (
    <button
      className={cx(
        "ui-button",
        `ui-button--${variant}`,
        size === "sm" && "ui-button--sm",
        block && "ui-button--block",
        className,
      )}
      {...props}
    >
      {children}
    </button>
  );
}

export function Field({ label, hint, error, children }: FieldProps) {
  return (
    <label className="ui-field">
      {label ? <span className="ui-field__label">{label}</span> : null}
      {children}
      {error ? <span className="ui-field__error">{error}</span> : hint ? <span className="ui-field__hint">{hint}</span> : null}
    </label>
  );
}

export function TextInput(props: TextInputProps) {
  return <input className={cx("ui-input", props.className)} {...props} />;
}

export function Textarea(props: TextareaProps) {
  return <textarea className={cx("ui-textarea", props.className)} {...props} />;
}

export function StateCard({ eyebrow, title, description, tone = "info", action }: StateCardProps) {
  if (tone === "loading") {
    return (
      <section className="state-card state-card--loading" aria-label="Loading" aria-busy="true">
        <div className="ui-skeleton" aria-hidden="true">
          <span className="ui-skeleton__line ui-skeleton__line--lg" />
          <span className="ui-skeleton__line ui-skeleton__line--md" />
          <span className="ui-skeleton__line ui-skeleton__line--sm" />
        </div>
      </section>
    );
  }

  return (
    <section className="state-card">
      <div className={cx("ui-state", `ui-state--${tone}`)}>
        <span className="ui-state__icon" aria-hidden="true">{toneIconMap[tone]}</span>
        <div>
          <p className="section-kicker">{eyebrow}</p>
          <h2 className="ui-state__title">{title}</h2>
          {description ? <p className="ui-state__description">{description}</p> : null}
          {action ? <div className="page-intro__actions">{action}</div> : null}
        </div>
      </div>
    </section>
  );
}

export function InlineNotice({ title, tone = "info" }: NoticeProps) {
  return (
    <div className={cx("ui-notice", `ui-notice--${tone}`)}>
      <span className="ui-notice__icon" aria-hidden="true">{toneIconMap[tone]}</span>
      <div>
        <h3 className="ui-notice__title">{title}</h3>
      </div>
    </div>
  );
}

export function ToastStack({ items, onClose }: { items: ToastItem[]; onClose?: (id: string) => void }) {
  return (
    <div className="toast-stack" aria-live="polite">
      {items.map((item) => (
        <div key={item.id} className={cx("toast-card", `toast-card--${item.tone ?? "info"}`)}>
          <span className="toast-card__icon" aria-hidden="true">{toneIconMap[item.tone ?? "info"]}</span>
          <div>
            <h3 className="toast-card__title">{item.title}</h3>
            <p className="toast-card__description">{item.description}</p>
          </div>
          <button className="toast-card__close" type="button" aria-label="?リ린" onClick={() => onClose?.(item.id)}>x</button>
        </div>
      ))}
    </div>
  );
}

export function MetricGrid({ items }: { items: Array<{ label: string; value: string; }> }) {
  return (
    <div className="ui-metric-grid">
      {items.map((item) => (
        <div key={item.label} className="ui-metric">
          <p className="ui-metric__label">{item.label}</p>
          <p className="ui-metric__value">{item.value}</p>
        </div>
      ))}
    </div>
  );
}

export function TableCard({ eyebrow, title, description, actions, children }: TableCardProps) {
  return (
    <section className="ui-table">
      <div className="ui-table__header">
        <div>
          <p className="ui-eyebrow">{eyebrow}</p>
          <h3 className="ui-table__title">{title}</h3>
          {description ? <p className="ui-table__description">{description}</p> : null}
        </div>
        {actions}
      </div>
      <div className="ui-table__inner">{children}</div>
    </section>
  );
}

export function SidePanel({ eyebrow, title, description, footer, tone = "default", children }: SidePanelProps) {
  return (
    <aside className={cx("ui-side-panel", tone === "gray" && "ui-side-panel--gray")}>
      <div className="ui-panel-header">
        <div>
          <p className="ui-eyebrow">{eyebrow}</p>
          <h3 className="ui-panel-title">{title}</h3>
          {description ? <p className="ui-panel-description">{description}</p> : null}
        </div>
      </div>
      {children}
      {footer ? <div>{footer}</div> : null}
    </aside>
  );
}

export function CalendarCard({ eyebrow, title, description, weekdays, days }: CalendarCardProps) {
  return (
    <section className="ui-calendar ui-calendar--gray">
      <div className="ui-calendar__header">
        <div>
          <p className="ui-eyebrow">{eyebrow}</p>
          <h3 className="ui-calendar__title">{title}</h3>
          {description ? <p className="ui-calendar__description">{description}</p> : null}
        </div>
      </div>
      <div className="ui-calendar__weekday">
        {weekdays.map((weekday) => (
          <span key={weekday}>{weekday}</span>
        ))}
      </div>
      <div className="ui-calendar__days">
        {days.map((day, index) => (
          <div
            key={`${day.dateLabel}-${index}`}
            className={cx(
              "ui-calendar__day",
              day.isPresent && "ui-calendar__day--present",
              day.isMuted && "ui-calendar__day--muted",
              day.isToday && "ui-calendar__day--today",
            )}
          >
            <span className="ui-calendar__date">{day.dateLabel}</span>
            <span className="ui-calendar__status">{day.statusLabel}</span>
          </div>
        ))}
      </div>
    </section>
  );
}

export function Divider(props: ComponentPropsWithoutRef<"hr">) {
  return <hr className="ui-divider" {...props} />;
}
