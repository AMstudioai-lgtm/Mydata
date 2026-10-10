-- Schéma Supabase v1.1.0 pour Mydata
--
-- À exécuter une fois dans le SQL Editor du projet ieqafnstztrswxmrbxob.supabase.co
-- Dashboard > SQL Editor > New query > coller > Run.
--
-- Point important : les colonnes updated_at / deleted_at / user_id doivent porter exactement
-- ces noms, car le client Android les lit et les écrit par leur nom via PostgREST.

-- ---------------------------------------------------------------- journals
create table if not exists public.journals (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null default auth.uid(),
    name        text not null,
    created_at  bigint not null,
    updated_at  bigint not null default 0,
    deleted_at  bigint
);

-- -------------------------------------------------------------- properties
create table if not exists public.properties (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null default auth.uid(),
    journal_id   uuid not null references public.journals(id) on delete cascade,
    name         text not null,
    kind         text not null check (kind in ('text', 'number', 'date', 'select')),
    options_json text not null default '[]',
    sort_order   integer not null default 0,
    format       text check (format is null or format in ('plain', 'percent', 'dollar')),
    updated_at   bigint not null default 0,
    deleted_at   bigint
);

-- Migration depuis un schema anterieur a la v1.2.0 : la colonne n'existe pas encore.
-- Sans valeur par defaut, les lignes existantes restent NULL, ce qui vaut 'plain'.
alter table public.properties add column if not exists format text;
alter table public.properties drop constraint if exists properties_format_check;
alter table public.properties add constraint properties_format_check
    check (format is null or format in ('plain', 'percent', 'dollar'));

-- -------------------------------------------------------------- entry_rows
create table if not exists public.entry_rows (
    id          uuid primary key default gen_random_uuid(),
    user_id     uuid not null default auth.uid(),
    journal_id  uuid not null references public.journals(id) on delete cascade,
    values_json text not null default '{}',
    created_at  bigint not null,
    updated_at  bigint not null default 0,
    deleted_at  bigint
);

-- Index utilisés par les requêtes de synchro (filtre user_id + curseur updated_at).
create index if not exists journals_user_updated_idx   on public.journals   (user_id, updated_at);
create index if not exists properties_user_updated_idx on public.properties (user_id, updated_at);
create index if not exists entry_rows_user_updated_idx on public.entry_rows (user_id, updated_at);

-- RLS : chaque utilisateur ne voit et ne modifie que ses propres lignes.
-- Les identifiants anonymes (clé publiable) ne donnent aucun accès sans session valide.
alter table public.journals   enable row level security;
alter table public.properties enable row level security;
alter table public.entry_rows enable row level security;

drop policy if exists "journals owner"   on public.journals;
drop policy if exists "properties owner" on public.properties;
drop policy if exists "entry_rows owner" on public.entry_rows;

create policy "journals owner" on public.journals
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "properties owner" on public.properties
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "entry_rows owner" on public.entry_rows
    for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
