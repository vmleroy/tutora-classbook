CREATE TABLE `group_class` (
	`id` text PRIMARY KEY NOT NULL,
	`place_id` text NOT NULL,
	`name` text NOT NULL,
	`subject` text,
	`rate_cents` integer,
	`archived_at` text,
	`created_at` text NOT NULL,
	`updated_at` text NOT NULL,
	FOREIGN KEY (`place_id`) REFERENCES `place`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `group_class_place` ON `group_class` (`place_id`);--> statement-breakpoint
CREATE TABLE `group_member` (
	`group_id` text NOT NULL,
	`student_id` text NOT NULL,
	PRIMARY KEY(`group_id`, `student_id`),
	FOREIGN KEY (`group_id`) REFERENCES `group_class`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`student_id`) REFERENCES `student`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `place` (
	`id` text PRIMARY KEY NOT NULL,
	`builtin` text,
	`name` text,
	`address` text,
	`kind` text NOT NULL,
	`payer` text NOT NULL,
	`rate_type` text NOT NULL,
	`rate_cents` integer,
	`commission_bp` integer DEFAULT 0 NOT NULL,
	`pay_cycle` text NOT NULL,
	`monthly_salary_cents` integer,
	`default_duration_min` integer DEFAULT 60 NOT NULL,
	`travel_mode` text DEFAULT 'none' NOT NULL,
	`travel_fee_cents` integer DEFAULT 0 NOT NULL,
	`archived_at` text,
	`created_at` text NOT NULL,
	`updated_at` text NOT NULL,
	CONSTRAINT "place_commission" CHECK("place"."commission_bp" BETWEEN 0 AND 9000)
);
--> statement-breakpoint
CREATE UNIQUE INDEX `place_builtin_unique` ON `place` (`builtin`);--> statement-breakpoint
CREATE TABLE `student` (
	`id` text PRIMARY KEY NOT NULL,
	`name` text NOT NULL,
	`subject` text,
	`phone` text,
	`notes` text,
	`color` text NOT NULL,
	`rate_type` text NOT NULL,
	`archived_at` text,
	`created_at` text NOT NULL,
	`updated_at` text NOT NULL
);
--> statement-breakpoint
CREATE TABLE `student_place` (
	`student_id` text NOT NULL,
	`place_id` text NOT NULL,
	`is_default` integer DEFAULT false NOT NULL,
	`rate_cents` integer,
	`created_at` text NOT NULL,
	`updated_at` text NOT NULL,
	PRIMARY KEY(`student_id`, `place_id`),
	FOREIGN KEY (`student_id`) REFERENCES `student`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`place_id`) REFERENCES `place`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE INDEX `student_place_place` ON `student_place` (`place_id`);--> statement-breakpoint
CREATE UNIQUE INDEX `student_place_one_default` ON `student_place` (`student_id`) WHERE "student_place"."is_default" = 1;