CREATE TABLE `lesson` (
	`id` text PRIMARY KEY NOT NULL,
	`rule_id` text,
	`occurrence_date` text,
	`student_id` text,
	`group_id` text,
	`place_id` text NOT NULL,
	`start_at` text NOT NULL,
	`planned_min` integer NOT NULL,
	`status` text DEFAULT 'scheduled' NOT NULL,
	`value_cents` integer,
	`travel_fee_cents` integer,
	`is_detached` integer DEFAULT false NOT NULL,
	`notes` text,
	`created_at` text NOT NULL,
	`updated_at` text NOT NULL,
	FOREIGN KEY (`rule_id`) REFERENCES `schedule_rule`(`id`) ON UPDATE no action ON DELETE set null,
	FOREIGN KEY (`student_id`) REFERENCES `student`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`group_id`) REFERENCES `group_class`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`place_id`) REFERENCES `place`(`id`) ON UPDATE no action ON DELETE cascade,
	CONSTRAINT "lesson_who" CHECK(("lesson"."student_id" IS NULL) <> ("lesson"."group_id" IS NULL))
);
--> statement-breakpoint
CREATE UNIQUE INDEX `lesson_occurrence` ON `lesson` (`rule_id`,`occurrence_date`);--> statement-breakpoint
CREATE INDEX `lesson_start` ON `lesson` (`start_at`);--> statement-breakpoint
CREATE INDEX `lesson_student_start` ON `lesson` (`student_id`,`start_at`);--> statement-breakpoint
CREATE INDEX `lesson_place_start` ON `lesson` (`place_id`,`start_at`);--> statement-breakpoint
CREATE TABLE `rule_skip` (
	`rule_id` text NOT NULL,
	`occurrence_date` text NOT NULL,
	PRIMARY KEY(`rule_id`, `occurrence_date`),
	FOREIGN KEY (`rule_id`) REFERENCES `schedule_rule`(`id`) ON UPDATE no action ON DELETE cascade
);
--> statement-breakpoint
CREATE TABLE `schedule_rule` (
	`id` text PRIMARY KEY NOT NULL,
	`student_id` text,
	`group_id` text,
	`place_id` text NOT NULL,
	`weekday` integer NOT NULL,
	`start_time` text NOT NULL,
	`duration_min` integer NOT NULL,
	`repeat` text NOT NULL,
	`starts_on` text NOT NULL,
	`ends_on` text,
	`time_zone` text NOT NULL,
	`value_cents` integer,
	`travel_fee_cents` integer,
	`generated_until` text NOT NULL,
	`created_at` text NOT NULL,
	`updated_at` text NOT NULL,
	FOREIGN KEY (`student_id`) REFERENCES `student`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`group_id`) REFERENCES `group_class`(`id`) ON UPDATE no action ON DELETE cascade,
	FOREIGN KEY (`place_id`) REFERENCES `place`(`id`) ON UPDATE no action ON DELETE cascade,
	CONSTRAINT "schedule_rule_who" CHECK(("schedule_rule"."student_id" IS NULL) <> ("schedule_rule"."group_id" IS NULL))
);
