---
name: BuyPilot-AI
description: AI guided toy shopping assistant for Android
colors:
  primary: "#FF6A3D"
  primary-soft: "#FFE5DA"
  surface-bg: "#F7F8FA"
  surface-card: "#FFFFFF"
  surface-muted: "#EEF1F5"
  border: "#E2E7EE"
  text-primary: "#1F2329"
  text-secondary: "#646A73"
  text-muted: "#8A919F"
  info: "#3B82F6"
  success: "#22C55E"
  warning: "#F5A524"
  attention: "#FFF0F2"
typography:
  display:
    fontFamily: "System"
    fontSize: "22px"
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: "0"
  title:
    fontFamily: "System"
    fontSize: "18px"
    fontWeight: 600
    lineHeight: 1.25
    letterSpacing: "0"
  body:
    fontFamily: "System"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: "0"
  label:
    fontFamily: "System"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: 1.3
    letterSpacing: "0"
rounded:
  sm: "8px"
  md: "12px"
  lg: "16px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.surface-card}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
  button-secondary:
    backgroundColor: "{colors.surface-muted}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.md}"
    padding: "12px 16px"
  input:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.lg}"
    padding: "12px 14px"
  card:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.md}"
    padding: "16px"
---

# Design System: BuyPilot-AI

## 1. Overview

**Creative North Star: "The Guided Decision Loop"**

This system should feel like a calm, trustworthy assistant that turns uncertain shopping intent into a structured decision. The conversation is the surface, but the actual product is a decision machine: light in motion, dense in information, and never cluttered by unnecessary chrome.

It explicitly rejects marketplace noise, long shopping pages, decorative hero composition, and anything that makes the interface feel like a generic SaaS template. The UI should stay conversational and compact, with summary faces in the main timeline and deeper detail pushed into bottom sheets.

**Key Characteristics:**
- calm and credible
- light, warm, and mobile-first
- summary-first in the timeline
- detail in bottom sheets
- decision-oriented, not catalog-oriented

## 2. Colors

Warm coral and soft neutral surfaces carry the system.

### Primary
- **Coral Orange** (#FF6A3D): primary CTA, active state, send action, key emphasis.

### Neutral
- **Cloud White** (#F7F8FA): page background.
- **Pure White** (#FFFFFF): cards and sheets.
- **Mist Gray** (#EEF1F5): inputs and secondary surfaces.
- **Divider Gray** (#E2E7EE): borders and separators.
- **Ink Black** (#1F2329): primary text.
- **Slate Gray** (#646A73): secondary text.
- **Muted Gray** (#8A919F): helper text and placeholders.

### Semantic
- **Sky Blue** (#3B82F6): evidence links and info accents.
- **Mint Green** (#22C55E): safe and confirmed state.
- **Amber Honey** (#F5A524): caution and risk.
- **Rose Tint** (#FFF0F2): clarification and gentle attention.
- **Peach Tint** (#FFE5DA): soft highlights and chips.

**The One Accent Rule.** Coral orange is the primary accent and should stay rare enough to feel intentional.

## 3. Typography

**Display Font:** system sans
**Body Font:** system sans
**Label/Mono Font:** system sans

The system should feel native to Android, not editorial or ornamental. Hierarchy comes from weight and spacing, not from exaggerated scale shifts.

### Hierarchy
- **Display** (600, 22px, 1.2): page titles and major summary faces.
- **Title** (600, 18px, 1.25): card headers and section leads.
- **Body** (400, 14px, 1.5): conversational copy and card body text.
- **Label** (500, 12px, 1.3): chips, actions, and metadata.

**The No Display Font Rule.** Labels, buttons, and data should never feel decorative.

## 4. Elevation

This system uses shallow layering rather than heavy shadow drama. Depth comes from spacing, borders, and card contrast, not from dark blur.

### Shadow Vocabulary
- **Ambient Low**: small, soft shadow for cards and sheets.
- **Focused Lift**: subtle emphasis for active cards and overlays.

**The Flat-by-Default Rule.** Surfaces rest flat unless interaction needs a lift.

## 5. Components

### Buttons
Compact, rounded, and direct. Primary actions use coral orange. Secondary actions stay neutral and quiet.

### Cards
Rounded, white, padded, and scannable. Summary cards belong in the timeline. Detail belongs below the fold in bottom sheets.

### Inputs
Soft-filled, bottom-fixed, multiline, and easy to reach with one thumb.

### Chat Timeline
One vertical timeline. `thinking` and stream text update inline; structured results appear as summary cards or swipe deck items.

### Bottom Sheets
Used for criteria editing, product detail, evidence, and comparison.

## 6. Do's and Don'ts

### Do:
- Do keep the main timeline light and readable.
- Do use coral orange only for primary actions and active states.
- Do keep product detail and evidence in bottom sheets.
- Do preserve one time line, one composer, one decision flow.

### Don't:
- Don't make it look like a marketplace.
- Don't use nested cards.
- Don't put long evidence blocks directly in chat.
- Don't move the stop action into the top bar.
- Don't use dark, heavy, casino-like visuals.
