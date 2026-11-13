"""Generates demo field photos and the icon art with the Codex CLI image tool.

Codex saves images into ~/.codex/generated_images/<session>/; the image of the session named in the run's
output is copied to design/art/<id>.png. Already present images are skipped.

    python tools/gen_art.py            # everything missing
    python tools/gen_art.py cat1 icon  # only these
"""
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "design" / "art"
GEN = Path.home() / ".codex" / "generated_images"

HEAD = "Use your image generation tool to create ONE image. Do nothing else (no file copying).\n\n"
CAT = "a plump adult blue-grey British Shorthair cat with round copper-orange eyes and a dense plush coat"
DOG = "a young red-and-white Pembroke Welsh Corgi with big upright ears"
PHOTO = ("Style: an ordinary candid smartphone photo taken at home by the owner, natural window light, realistic, slightly imperfect framing, "
         "cosy lived-in flat in muted colours. Square 1:1. No text, no watermark, no people's faces.")

PHOTOS = {
    "cat1": f"{CAT} sitting in the 'loaf' pose on a wooden floor, paws tucked fully under its body, eyes half-closed.",
    "cat2": f"{CAT} lying on its back on a sofa, belly up, front paws curled, relaxed and trusting.",
    "cat3": f"{CAT} sitting on a windowsill seen from behind and slightly to the side, watching pigeons outside, ears forward.",
    "cat4": f"{CAT} asleep curled up inside a small cardboard box that is clearly too small for it, on a rug.",
    "cat5": f"{CAT} close-up of its face, eyes in a slow half-closed blink, looking at the camera, soft focus background.",
    "cat6": f"{CAT} stretched out long in a patch of sunlight on the floor, front legs extended, enjoying the sun.",
    "cat7": f"{CAT} kneading a soft knitted blanket with its front paws, eyes squinting with pleasure.",
    "dog1": f"{DOG} in a play bow on a living-room rug: front legs down, rear up, tail wagging, a rope toy in front of it.",
    "dog2": f"{DOG} sitting and tilting its head to one side, looking at the camera curiously, in a hallway.",
    "dog3": f"{DOG} lying in a 'sploot' on a kitchen floor: belly flat, both hind legs stretched straight out behind.",
}

ICON = HEAD + """Image: an app icon artwork, square 1:1, for an app where your pet is an envoy from another planet studied by a 1960s bureau of xenobiology.
A friendly cat head drawn as a mid-century space-age emblem: simple flat shapes, cream-white cat head with big round eyes glowing phosphor green,
a thin green orbit ring crossing behind it like a planet's ring, a small red-orange star, on a flat deep teal (#17312E) background.
Clean vector-like flat illustration, bold simple shapes that read at small size, no gradients, no text, no letters, the motif centred with generous margins (it will be cropped to a circle)."""

STYLE = ("Style: flat mid-century space-age illustration like a 1960s space-race poster or postcard: bold simple rounded shapes, "
         "a limited palette of deep violet, rocket orange, warm cream, peach and soft gold (plus the colours named for each object), subtle print grain, "
         "clean crisp edges, gentle highlights, no outlines heavier than needed. No text, no letters, no numbers anywhere.")
MAGENTA = ("Background: flat solid pure magenta (#FF00FF) everywhere around the objects, with no gradient, no stars, no glow and no shadow on it. "
           "Do not use magenta or hot pink inside the objects.")

PLANETS = """Image: a sprite sheet of six small planets, the home worlds of pets that secretly come from space.
Layout: 3 columns x 2 rows of equal square cells; one planet per cell, centred, the same size, with a clear margin; nothing touches the cell borders; no grid lines.
In order, left to right, top to bottom:
1 FELIS, the cat planet: warm amber and cream, with two pointed cat-ear mountains on top, a soft ring like a strand of yarn, a cosy sleepy feel;
2 CANIS, the dog planet: ochre and chestnut brown with paw-print craters and a little bone-shaped moon;
3 PSITTACUS, the parrot planet: lush jungle green and turquoise with feather-shaped clouds and a ring in rainbow parrot colours;
4 GLIRES, the rabbit and rodent planet: golden wheat and meadow green with round burrow holes, tufts of grass and a small carrot-orange moon;
5 MUSTELA, the ferret planet: dusty lilac and grey-brown, laced with winding tunnels, a small striped moon;
6 SQUAMATA, the reptile planet: olive and terracotta with a scaly texture, small volcanoes and a hot little sun beside it.
""" + STYLE + " " + MAGENTA

HERO = """Image: the mascot of an app where your pet is an envoy from another planet: a plump friendly grey cat in a retro 1960s astronaut suit
(cream suit with orange details) and a clear round bubble helmet, floating and waving one paw, a tiny antenna on the helmet, big curious eyes.
Square 1:1, the character centred with a generous margin.
""" + STYLE + " " + MAGENTA

RANKS = """Image: a sprite sheet of eight enamel lapel badges for the ranks of a 1960s bureau of xenobiology, each one grander than the last.
Layout: 4 columns x 2 rows of equal square cells; one round badge per cell, centred, the same size, clear margin; nothing touches the cell borders; no grid lines.
In order: 1 a simple bronze badge with a glass flask; 2 bronze with a magnifying glass; 3 silver with a microscope; 4 silver with a telescope;
5 gold with a rocket; 6 gold with a satellite; 7 gold with a comet and a small laurel; 8 a grand gold badge with a ringed planet, a laurel wreath and a star.
Shiny enamel in violet, orange and cream inside metal rims.
""" + STYLE + " " + MAGENTA

ICON2 = HEAD + """Image: an app icon artwork, square 1:1, for an app where your pet is an envoy from another planet studied by a 1960s bureau of xenobiology.
A friendly cream-white cat head drawn as a mid-century space-age emblem with big round glowing amber eyes, a thin orange orbit ring crossing behind it like a planet's ring,
a small golden four-pointed star, on a flat deep violet (#1C1642) background with a faint glow behind the head.
""" + STYLE + " The motif centred with generous margins (it will be cropped to a circle)."

ENGRAVE = ("Style: a fine antique copper-plate engraving from an 18th-century celestial atlas, drawn ONLY in thin warm gold lines (#C9A86A) "
           "with delicate hatching and stippling, elegant and restrained, no fills of other colours, no text, no letters, no numbers. "
           "Background: flat solid very dark graphite (#12161C), nothing else on it.")

SAMPLE = HEAD + """Image: a plate from an old celestial atlas showing FELIS, the home planet of cats: a sphere with two gentle cat-ear mountains on its horizon,
a soft ring like a strand of yarn, tiny constellations and a few small stars around it, a thin decorative circular frame. Square 1:1, centred, generous margin.
""" + ENGRAVE

JOBS = {k: HEAD + "Image: " + v + "\n" + PHOTO for k, v in PHOTOS.items()}
JOBS["sample_engrave"] = SAMPLE

E_PLANETS = {
    "e_canis": "CANIS, the home planet of dogs: a sphere with paw-print craters, a little bone-shaped moon on an orbit line",
    "e_psittacus": "PSITTACUS, the home planet of parrots: a jungle sphere with feather-shaped clouds and a ring made of long feathers",
    "e_glires": "GLIRES, the home planet of rabbits and rodents: a meadow sphere with round burrow holes, ears of wheat and a small carrot-shaped moon",
    "e_mustela": "MUSTELA, the home planet of ferrets: a sphere laced with winding tunnels and a small striped moon",
    "e_squamata": "SQUAMATA, the home planet of reptiles: a sphere with a scaly texture, small volcanoes and a little radiant sun beside it",
}
for k, v in E_PLANETS.items():
    JOBS[k] = HEAD + "Image: a plate from an old celestial atlas showing " + v + ", tiny constellations and a few small stars around it, a thin decorative circular frame. Square 1:1, centred, generous margin. " + ENGRAVE
JOBS["e_ranks"] = HEAD + """Image: eight engraved medals for the ranks of an old bureau of xenobiology, each grander than the last.
Layout: 4 columns x 2 rows of equal square cells; one round medal per cell, centred, the same size, clear margin; nothing touches the cell borders; no grid lines.
In order: 1 a plain medal with a glass flask; 2 a magnifying glass; 3 a microscope; 4 a telescope; 5 a rocket; 6 a satellite; 7 a comet with a small laurel sprig; 8 a grand medal with a ringed planet, a full laurel wreath and a star.
""" + ENGRAVE
JOBS["e_menu"] = HEAD + """Image: six small engraved emblems for the actions of an observer's journal, each in a thin circular frame.
Layout: 3 columns x 2 rows of equal square cells; one emblem per cell, centred, the same size, clear margin; nothing touches the cell borders; no grid lines.
In order: 1 a magnifying glass over a paw print (observation); 2 an old bellows camera (field photo); 3 an ornate empty picture frame (photo from the album);
4 a glass flask with a little sprig of catnip inside (experiment); 5 a pocket watch with a small check mark (life support routine); 6 a quill pen over an open notebook (field note).
""" + ENGRAVE
ICON_BASE = ("Image: an Android app icon, square 1:1, for «Specimen», an app where your pet is a little envoy from another planet observed through an old observatory. "
             "Bold, simple and iconic so it reads at 48 pixels: few strong gold lines and solid gold shapes (#C9A86A) on flat very dark graphite (#12161C), "
             "elegant like an emblem from an old celestial atlas, the motif centred and filling about 60% of the square (it will be cropped to a circle). No text, no letters, no frame around the whole square. ")
JOBS["icon_a"] = HEAD + ICON_BASE + "Motif: the silhouette of a sitting cat seen from behind, looking up at a large crescent moon with a small ringed planet and two stars."
JOBS["icon_b"] = HEAD + ICON_BASE + "Motif: a cat's head whose two ears rise like mountain peaks over the curve of a planet, with a thin orbit ring around it and a small four-pointed star."
JOBS["icon_c"] = HEAD + ICON_BASE + "Motif: a round brass telescope lens (a porthole) with a cat's face peeking through it, a small ringed planet reflected in the glass."
JOBS["e_appetite"] = HEAD + """Image: four small engraved scenes about a cat's appetite, each a clear little story, the same cat and the same food bowl in all four.
Layout: 4 columns x 1 row of equal square cells; one scene per cell, centred, same scale, clear margin; nothing touches the cell borders; no grid lines, no frames.
In order, left to right:
1 ASKED FOR MORE: an empty, licked-clean bowl, the cat sitting next to it licking its lips and looking up hopefully, a tiny heart above;
2 AS USUAL: an empty bowl, the cat walking away contentedly with its tail up;
3 ATE LITTLE: the bowl still more than half full of food, the cat sniffing it without interest;
4 REFUSED: a full untouched bowl heaped with food, the cat sitting with its back turned to it.
""" + ENGRAVE
JOBS["e_appetite_dog"] = JOBS["e_appetite"].replace("a cat's appetite", "a dog's appetite").replace("the same cat", "the same small dog (a corgi)").replace("the cat", "the dog").replace("licking its lips and looking up hopefully", "licking its nose, tail wagging, looking up hopefully").replace("walking away contentedly with its tail up", "trotting away contentedly, tail wagging")
JOBS["e_appetite_any"] = HEAD + """Image: four small engraved still lifes of one ornate food dish that tell how an animal ate, with NO animal in them; the same dish in all four, each state obvious at a glance.
Layout: 4 columns x 1 row of equal square cells; one still life per cell, centred, same scale, clear margin; nothing touches the cell borders; no grid lines, no frames.
In order, left to right:
1 ASKED FOR MORE: the dish completely empty and licked shiny, a few sparkles over it and a small heart above, an empty spoon leaning on it;
2 AS USUAL: the dish empty with just a few crumbs left, a neat napkin beside it;
3 ATE LITTLE: the dish still two-thirds full, a few pieces scattered beside it;
4 REFUSED: the dish heaped high and untouched, with a thin cobweb and a small dry leaf on the food.
""" + ENGRAVE
_APP = JOBS["e_appetite"]
JOBS["e_appetite_bird"] = _APP.replace("a cat's appetite", "a parrot's appetite").replace("the same cat and the same food bowl", "the same small parrot (a cockatiel) and the same seed cup").replace("the same cat", "the same parrot").replace("the cat", "the parrot").replace("bowl", "seed cup").replace("licking its lips and looking up hopefully", "chirping happily with its crest up").replace("walking away contentedly with its tail up", "hopping away contentedly along a perch").replace("sitting with its back turned to it", "perched with its back turned to it, crest down")
JOBS["e_appetite_rabbit"] = _APP.replace("a cat's appetite", "a rabbit's appetite").replace("the same cat and the same food bowl", "the same rabbit and the same bowl of greens and hay").replace("the same cat", "the same rabbit").replace("the cat", "the rabbit").replace("licking its lips and looking up hopefully", "standing up on its hind legs, nose twitching hopefully").replace("walking away contentedly with its tail up", "hopping away contentedly").replace("sitting with its back turned to it", "hunched with its back turned to it")
JOBS["e_appetite_ferret"] = _APP.replace("a cat's appetite", "a ferret's appetite").replace("the same cat", "the same ferret").replace("the cat", "the ferret").replace("walking away contentedly with its tail up", "bouncing away happily")
JOBS["e_appetite_lizard"] = _APP.replace("a cat's appetite", "a gecko's appetite").replace("the same cat and the same food bowl", "the same leopard gecko and the same shallow dish of insects").replace("the same cat", "the same gecko").replace("the cat", "the gecko").replace("licking its lips and looking up hopefully", "licking its lips, head raised hopefully").replace("walking away contentedly with its tail up", "walking away contentedly, belly round").replace("sitting with its back turned to it", "lying with its back turned to it")
JOBS["e_busts"] = HEAD + """Image: six engraved portrait busts for a naturalist's atlas, each the head and shoulders of one animal facing three-quarters, calm and friendly, filling a round cameo.
Layout: 3 columns x 2 rows of equal square cells; one portrait per cell, centred, the same size, with a clear margin; nothing touches the cell borders; no grid lines, no frames around the cells.
In order, left to right, top to bottom: 1 a cat; 2 a dog (a friendly mixed breed with soft ears); 3 a parrot (a cockatiel with its crest); 4 a rabbit; 5 a ferret; 6 a leopard gecko.
Each portrait fills its cell like a medallion (no circular frame line), detailed fur, feather and scale hatching.
""" + ENGRAVE
JOBS["e_hero"] = HEAD + """Image: an engraving of the envoy: a plump friendly cat in an old-fashioned diving-style space suit with a round glass helmet, floating among small stars and waving one paw, a tiny antenna on the helmet. Square 1:1, centred, generous margin.
""" + ENGRAVE
JOBS["e_icon"] = HEAD + """Image: an app icon, square 1:1: a friendly cat head drawn as a fine gold engraving inside a thin circular orbit with a small four-pointed star, on flat very dark graphite (#12161C). Bold enough to read at small size, centred with generous margins (it will be cropped to a circle). No text.
""" + ENGRAVE
JOBS.update({"planets": HEAD + PLANETS, "hero": HEAD + HERO, "ranks": HEAD + RANKS, "icon": ICON2})


def image_of(output):
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(job):
    name, prompt = job
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name, flush=True)
        return
    r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", errors="replace", capture_output=True, cwd=str(ROOT))
    p = image_of((r.stdout or "") + (r.stderr or ""))
    if p is None:
        print("FAIL", name, (r.stdout or "")[-300:], (r.stderr or "")[-300:], flush=True)
        return
    shutil.copy(p, dst)
    print("ok", name, flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = [j for j in JOBS.items() if not want or j[0] in want]
    with ThreadPoolExecutor(3) as ex:
        list(ex.map(run, jobs))


if __name__ == "__main__":
    main()
