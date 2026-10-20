-- ==============================================================================
-- NEXORA LEARN — SUPABASE DATABASE SCHEMA & ROW LEVEL SECURITY (RLS) POLICIES
-- Target: Gujarat Board Class 12 Commerce Smart Study Planner
-- Project URL: https://mysnralzdpusuuquhxlf.supabase.co
-- ==============================================================================

-- 1. Enable UUID Extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Student Profiles Table
-- Stores authenticated student profile, target score goals, study streak, and focused minutes.
CREATE TABLE IF NOT EXISTS public.student_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    student_id TEXT NOT NULL UNIQUE,
    student_name TEXT NOT NULL,
    email TEXT,
    phone_number TEXT,
    photo_url TEXT DEFAULT '',
    medium TEXT DEFAULT 'Gujarati Medium',
    class_level TEXT DEFAULT 'Class 12 Commerce',
    target_marks TEXT DEFAULT '90+ Marks',
    avatar_color_hex TEXT DEFAULT '#00BCD4',
    completed_todo_count INT DEFAULT 0,
    study_streak_days INT DEFAULT 0,
    competition_points INT DEFAULT 0,
    completed_chapter_ids TEXT[] DEFAULT '{}',
    total_focused_minutes INT DEFAULT 0,
    completed_sessions_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Safe migration script to alter avatar_color_hex from BIGINT to TEXT if present
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'student_profiles' 
          AND column_name = 'avatar_color_hex' 
          AND data_type = 'bigint'
    ) THEN
        ALTER TABLE public.student_profiles 
        ALTER COLUMN avatar_color_hex TYPE TEXT USING avatar_color_hex::text;
        ALTER TABLE public.student_profiles 
        ALTER COLUMN avatar_color_hex SET DEFAULT '#00BCD4';
    END IF;
END $$;

-- 3. Study Tasks Table (Student-created To-Dos)
CREATE TABLE IF NOT EXISTS public.study_tasks (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    subject_id TEXT DEFAULT '',
    subject_name TEXT DEFAULT '',
    chapter_id TEXT DEFAULT '',
    chapter_title TEXT DEFAULT '',
    exam_phase TEXT DEFAULT 'SCHOOL_EXAM',
    scheduled_date_str TEXT DEFAULT '',
    scheduled_date_formatted TEXT DEFAULT '',
    is_completed BOOLEAN DEFAULT false,
    completed_at BIGINT,
    is_missed BOOLEAN DEFAULT false,
    priority TEXT DEFAULT 'MEDIUM',
    duration_minutes INT DEFAULT 30,
    notes TEXT DEFAULT '',
    reminder_time TEXT,
    reminder_date_str TEXT,
    reminder_epoch_millis BIGINT,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. Student Study Milestones Table (Chapter syllabus progress)
CREATE TABLE IF NOT EXISTS public.student_milestones (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    subject_id TEXT NOT NULL,
    chapter_id TEXT NOT NULL,
    is_completed BOOLEAN DEFAULT false,
    completed_at BIGINT,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 5. Chat Conversations & Messages Tables (WhatsApp-inspired Chat)
CREATE TABLE IF NOT EXISTS public.conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.conversation_participants (
    conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.messages (
    id TEXT PRIMARY KEY,
    conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 6. Indexes for High-Performance Queries
CREATE INDEX IF NOT EXISTS idx_student_profiles_student_id ON public.student_profiles(student_id);
CREATE INDEX IF NOT EXISTS idx_study_tasks_user_id ON public.study_tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_study_tasks_scheduled_date ON public.study_tasks(scheduled_date_str);
CREATE INDEX IF NOT EXISTS idx_student_milestones_user_id ON public.student_milestones(user_id);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON public.messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversation_participants_user_id ON public.conversation_participants(user_id);

-- 7. Trigger to automatically update updated_at timestamps
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_student_profiles_updated_at ON public.student_profiles;
CREATE TRIGGER set_student_profiles_updated_at
    BEFORE UPDATE ON public.student_profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS set_study_tasks_updated_at ON public.study_tasks;
CREATE TRIGGER set_study_tasks_updated_at
    BEFORE UPDATE ON public.study_tasks
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS set_student_milestones_updated_at ON public.student_milestones;
CREATE TRIGGER set_student_milestones_updated_at
    BEFORE UPDATE ON public.student_milestones
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- ==============================================================================
-- 8. ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

-- Enable RLS on all tables
ALTER TABLE public.student_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.study_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_milestones ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;

-- Policies for public.student_profiles (Own CRUD + Public read for Leaderboard)
DROP POLICY IF EXISTS "student_profiles_select_own" ON public.student_profiles;
DROP POLICY IF EXISTS "student_profiles_select_all_authenticated" ON public.student_profiles;
CREATE POLICY "student_profiles_select_all_authenticated"
    ON public.student_profiles
    FOR SELECT
    TO authenticated
    USING (true);

DROP POLICY IF EXISTS "student_profiles_insert_own" ON public.student_profiles;
CREATE POLICY "student_profiles_insert_own"
    ON public.student_profiles
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "student_profiles_update_own" ON public.student_profiles;
CREATE POLICY "student_profiles_update_own"
    ON public.student_profiles
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "student_profiles_delete_own" ON public.student_profiles;
CREATE POLICY "student_profiles_delete_own"
    ON public.student_profiles
    FOR DELETE
    TO authenticated
    USING (auth.uid() = id);

-- Policies for public.study_tasks
DROP POLICY IF EXISTS "study_tasks_select_own" ON public.study_tasks;
CREATE POLICY "study_tasks_select_own" ON public.study_tasks FOR SELECT TO authenticated USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "study_tasks_insert_own" ON public.study_tasks;
CREATE POLICY "study_tasks_insert_own" ON public.study_tasks FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "study_tasks_update_own" ON public.study_tasks;
CREATE POLICY "study_tasks_update_own" ON public.study_tasks FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "study_tasks_delete_own" ON public.study_tasks;
CREATE POLICY "study_tasks_delete_own" ON public.study_tasks FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- Policies for public.student_milestones
DROP POLICY IF EXISTS "student_milestones_select_own" ON public.student_milestones;
CREATE POLICY "student_milestones_select_own" ON public.student_milestones FOR SELECT TO authenticated USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "student_milestones_insert_own" ON public.student_milestones;
CREATE POLICY "student_milestones_insert_own" ON public.student_milestones FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "student_milestones_update_own" ON public.student_milestones;
CREATE POLICY "student_milestones_update_own" ON public.student_milestones FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "student_milestones_delete_own" ON public.student_milestones;
CREATE POLICY "student_milestones_delete_own" ON public.student_milestones FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- Policies for public.conversations & participants & messages
DROP POLICY IF EXISTS "conversations_select_participant" ON public.conversations;
CREATE POLICY "conversations_select_participant"
    ON public.conversations
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.conversation_participants cp
            WHERE cp.conversation_id = id AND cp.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "conversations_insert_auth" ON public.conversations;
CREATE POLICY "conversations_insert_auth"
    ON public.conversations
    FOR INSERT
    TO authenticated
    WITH CHECK (true);

DROP POLICY IF EXISTS "participants_select_own" ON public.conversation_participants;
CREATE POLICY "participants_select_own"
    ON public.conversation_participants
    FOR SELECT
    TO authenticated
    USING (user_id = auth.uid() OR EXISTS (
        SELECT 1 FROM public.conversation_participants cp
        WHERE cp.conversation_id = conversation_id AND cp.user_id = auth.uid()
    ));

DROP POLICY IF EXISTS "participants_insert_auth" ON public.conversation_participants;
CREATE POLICY "participants_insert_auth"
    ON public.conversation_participants
    FOR INSERT
    TO authenticated
    WITH CHECK (true);

DROP POLICY IF EXISTS "messages_select_participant" ON public.messages;
CREATE POLICY "messages_select_participant"
    ON public.messages
    FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.conversation_participants cp
            WHERE cp.conversation_id = conversation_id AND cp.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "messages_insert_sender" ON public.messages;
CREATE POLICY "messages_insert_sender"
    ON public.messages
    FOR INSERT
    TO authenticated
    WITH CHECK (
        sender_id = auth.uid() AND
        EXISTS (
            SELECT 1 FROM public.conversation_participants cp
            WHERE cp.conversation_id = conversation_id AND cp.user_id = auth.uid()
        )
    );

-- 9. Friend Requests Table & RLS Policies
CREATE TABLE IF NOT EXISTS public.friend_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_sender_receiver UNIQUE (sender_id, receiver_id)
);

ALTER TABLE public.friend_requests ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "friend_requests_select" ON public.friend_requests;
CREATE POLICY "friend_requests_select"
    ON public.friend_requests FOR SELECT TO authenticated
    USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

DROP POLICY IF EXISTS "friend_requests_insert" ON public.friend_requests;
CREATE POLICY "friend_requests_insert"
    ON public.friend_requests FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = sender_id);

DROP POLICY IF EXISTS "friend_requests_update" ON public.friend_requests;
CREATE POLICY "friend_requests_update"
    ON public.friend_requests FOR UPDATE TO authenticated
    USING (auth.uid() = receiver_id OR auth.uid() = sender_id)
    WITH CHECK (auth.uid() = receiver_id OR auth.uid() = sender_id);

DROP POLICY IF EXISTS "friend_requests_delete" ON public.friend_requests;
CREATE POLICY "friend_requests_delete"
    ON public.friend_requests FOR DELETE TO authenticated
    USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

