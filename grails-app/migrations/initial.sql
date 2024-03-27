--
-- PostgreSQL database dump
--

-- Dumped from database version 14.10 (Ubuntu 14.10-0ubuntu0.22.04.1)
-- Dumped by pg_dump version 14.10 (Ubuntu 14.10-0ubuntu0.22.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: data-quality
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO "data-quality";

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: quality_category; Type: TABLE; Schema: public; Owner: data-quality
--

CREATE TABLE public.quality_category (
    id bigint NOT NULL,
    version bigint NOT NULL,
    display_order bigint NOT NULL,
    date_created timestamp without time zone NOT NULL,
    last_updated timestamp without time zone NOT NULL,
    name text NOT NULL,
    label text NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    quality_profile_id bigint,
    description text
);


ALTER TABLE public.quality_category OWNER TO "data-quality";

--
-- Name: quality_filter; Type: TABLE; Schema: public; Owner: data-quality
--

CREATE TABLE public.quality_filter (
    id bigint NOT NULL,
    version bigint NOT NULL,
    display_order bigint NOT NULL,
    date_created timestamp without time zone NOT NULL,
    quality_category_id bigint NOT NULL,
    filter character varying(255) NOT NULL,
    last_updated timestamp without time zone NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    description character varying(255) NOT NULL
);


ALTER TABLE public.quality_filter OWNER TO "data-quality";

--
-- Name: quality_profile; Type: TABLE; Schema: public; Owner: data-quality
--

CREATE TABLE public.quality_profile (
    id bigint NOT NULL,
    version bigint NOT NULL,
    display_order bigint NOT NULL,
    short_name text NOT NULL,
    date_created timestamp without time zone NOT NULL,
    contact_name text,
    last_updated timestamp without time zone NOT NULL,
    name text NOT NULL,
    is_default boolean NOT NULL,
    contact_email text,
    enabled boolean DEFAULT true NOT NULL,
    description text,
    user_id character varying(255)
);


ALTER TABLE public.quality_profile OWNER TO "data-quality";

--
-- Data for Name: quality_filter; Type: TABLE DATA; Schema: public; Owner: data-quality
--

--
-- Name: hibernate_sequence; Type: SEQUENCE SET; Schema: public; Owner: data-quality
--

SELECT pg_catalog.setval('public.hibernate_sequence', 780, true);


--
-- Name: quality_category quality_category_pkey; Type: CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_category
    ADD CONSTRAINT quality_category_pkey PRIMARY KEY (id);


--
-- Name: quality_filter quality_filter_pkey; Type: CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_filter
    ADD CONSTRAINT quality_filter_pkey PRIMARY KEY (id);


--
-- Name: quality_profile quality_profile_pkey; Type: CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_profile
    ADD CONSTRAINT quality_profile_pkey PRIMARY KEY (id);


--
-- Name: quality_category uk14f75e252f2d47078968e39550ad; Type: CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_category
    ADD CONSTRAINT uk14f75e252f2d47078968e39550ad UNIQUE (quality_profile_id, label);


--
-- Name: quality_profile uk_2pes92njx82pgnweogcv7hdou; Type: CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_profile
    ADD CONSTRAINT uk_2pes92njx82pgnweogcv7hdou UNIQUE (name);


--
-- Name: quality_profile uk_sd16exc6bi5kbdoxkajdcrygo; Type: CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_profile
    ADD CONSTRAINT uk_sd16exc6bi5kbdoxkajdcrygo UNIQUE (short_name);


--
-- Name: quality_category ukc900442340694bd5d390d8ff1bfb; Type: CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_category
    ADD CONSTRAINT ukc900442340694bd5d390d8ff1bfb UNIQUE (quality_profile_id, name);


--
-- Name: quality_filter ukf2998cd52e10e52c21dc72d5f16d; Type: CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_filter
    ADD CONSTRAINT ukf2998cd52e10e52c21dc72d5f16d UNIQUE (quality_category_id, filter);


--
-- Name: quality_category_date_created_idx; Type: INDEX; Schema: public; Owner: data-quality
--

CREATE INDEX quality_category_date_created_idx ON public.quality_category USING btree (date_created);


--
-- Name: quality_category_enabled_idx; Type: INDEX; Schema: public; Owner: data-quality
--

CREATE INDEX quality_category_enabled_idx ON public.quality_category USING btree (enabled);


--
-- Name: quality_filter_date_created_idx; Type: INDEX; Schema: public; Owner: data-quality
--

CREATE INDEX quality_filter_date_created_idx ON public.quality_filter USING btree (date_created);


--
-- Name: quality_filter_enabled_idx; Type: INDEX; Schema: public; Owner: data-quality
--

CREATE INDEX quality_filter_enabled_idx ON public.quality_filter USING btree (enabled);


--
-- Name: quality_profile_enabled_idx; Type: INDEX; Schema: public; Owner: data-quality
--

CREATE INDEX quality_profile_enabled_idx ON public.quality_profile USING btree (enabled);


--
-- Name: quality_filter fk91qjbbtfdsd6440x554iqudhn; Type: FK CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_filter
    ADD CONSTRAINT fk91qjbbtfdsd6440x554iqudhn FOREIGN KEY (quality_category_id) REFERENCES public.quality_category(id);


--
-- Name: quality_category fkj3w3axjt6n0hj1h6h9mqw3h9x; Type: FK CONSTRAINT; Schema: public; Owner: data-quality
--

ALTER TABLE ONLY public.quality_category
    ADD CONSTRAINT fkj3w3axjt6n0hj1h6h9mqw3h9x FOREIGN KEY (quality_profile_id) REFERENCES public.quality_profile(id);


--
-- PostgreSQL database dump complete
--

