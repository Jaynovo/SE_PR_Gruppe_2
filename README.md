# SE_PR_Gruppe_2

Gruppenteilnehmer: 
Thomas Brunnbauer
Markus Gaber
Jason Sajovic

Rollen:
tbd 

Zeitaufzeichnung:  
"| time xxh" bedeutet, dass alle Gruppenmitglieder an den Commit gearbeitet haben.  
"| [name] xxh" bedeutet, dass nur "name" an den Commit gearbeitet hat.  

Priority:
Descending; P0 is the most important, P2 the least important

Sizes:
Serves as a rough category for how long a task will take  
XS: Quick fixes (<1h)  
S: 1-3h  
M: 3-5h  
L: 5-8h  
XL: >8h  

Story Points:  
Based on Sizes, provide a clearer estimate of hours

Database access is objectoriented; Pass along objects!



Auswertungen
Thomas
git log --all --pretty=format:"%ad %an %s" --date=format:"%Y-%m-%d %H:%M:%S" --graph --decorate |
awk '
{
  sum = 0

  # Explizit "Thomas Xh":
  n = match($0, /Thomas ?([0-9]+([.,][0-9]+)?)h/)
  if(n) {
      val = substr($0, RSTART+7, RLENGTH-8)
      gsub(/,/, ".", val)
      sum += val+0
      printf "Explizit für Thomas gezählt: %s => +%s\n", $0, val
  }

  # "time Xh" für alle:
  n2 = match($0, /time:? ?([0-9]+([.,][0-9]+)?)h/)
  if(n2) {
      val = substr($0, RSTART+5, RLENGTH-6)
      gsub(/,/, ".", val)
      sum += val+0
      printf "Gemeinsam (time) für Thomas gezählt: %s => +%s\n", $0, val
  }

  if(sum>0) thomas+=sum
}
END { printf "\nThomas: %.2f h\n", thomas }'


Jason
git log --all --pretty=format:"%ad %an %s" --date=format:"%Y-%m-%d %H:%M:%S" --graph --decorate |
awk '
{
  sum = 0

  # Explizit "Jason Xh":
  n = match($0, /Jason ?([0-9]+([.,][0-9]+)?)h/)
  if(n) {
      val = substr($0, RSTART+6, RLENGTH-7)
      gsub(/,/, ".", val)
      sum += val+0
      printf "Explizit für Jason gezählt: %s => +%s\n", $0, val
  }

  # "time Xh" für alle:
  n2 = match($0, /time:? ?([0-9]+([.,][0-9]+)?)h/)
  if(n2) {
      val = substr($0, RSTART+5, RLENGTH-6)
      gsub(/,/, ".", val)
      sum += val+0
      printf "Gemeinsam (time) für Jason gezählt: %s => +%s\n", $0, val
  }

  if(sum>0) jason+=sum
}
END { printf "\nJason: %.2f h\n", jason }'



Markus
git log --all --pretty=format:"%ad %an %s" --date=format:"%Y-%m-%d %H:%M:%S" --graph --decorate |
awk '
{
  sum = 0

  # Explizit "Markus Xh":
  n = match($0, /Markus ?([0-9]+([.,][0-9]+)?)h/)
  if(n) {
      val = substr($0, RSTART+7, RLENGTH-8)
      gsub(/,/, ".", val)
      sum += val+0
      printf "Explizit für Markus gezählt: %s => +%s\n", $0, val
  }

  # "time Xh" für alle:
  n2 = match($0, /time:? ?([0-9]+([.,][0-9]+)?)h/)
  if(n2) {
      val = substr($0, RSTART+5, RLENGTH-6)
      gsub(/,/, ".", val)
      sum += val+0
      printf "Gemeinsam (time) für Markus gezählt: %s => +%s\n", $0, val
  }

  if(sum>0) markus+=sum
}
END { printf "\nMarkus: %.2f h\n", markus }'
